package vn.com.m1tech.cnn.appEtax.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets; 
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import kong.unirest.json.JSONObject; 
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import vn.com.m1tech.app.lib.common.Page;
import vn.com.m1tech.cnn.appEtax.common.Utils;
import vn.com.m1tech.cnn.appEtax.dao.EtaxDao;
import vn.com.m1tech.cnn.appEtax.dao.IdentifyInfoDao;
import vn.com.m1tech.cnn.appEtax.dao.TaxPersonInfoDao;
import vn.com.m1tech.cnn.appEtax.entities.FileNameUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.GIAODICH;
import vn.com.m1tech.cnn.appEtax.entities.GIAYTO;
import vn.com.m1tech.cnn.appEtax.entities.IdentifyInfo;
import vn.com.m1tech.cnn.appEtax.entities.NDDSHDR;
import vn.com.m1tech.cnn.appEtax.entities.PeriodUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.ROWCTIET;
import vn.com.m1tech.cnn.appEtax.entities.TaxPersonInfo;
import vn.com.m1tech.cnn.appEtax.entities.XmlTaxExptFile;
import vn.com.m1tech.cnn.appEtax.entities.XmlTaxExptFileFilterReq;
import vn.com.m1tech.cnn.common.entities.ListOfId;
import vn.com.m1tech.cnn.dao.SettingDao;
import vn.com.m1tech.cnn.dao.admin.MsbTokenDao;
import vn.com.m1tech.cnn.domain.admin.Setting;
import vn.com.m1tech.cnn.domain.admin.Users;
import vn.com.m1tech.cnn.domain.response.JsonResponeBase;
import vn.com.m1tech.cnn.sor.appEtax.common.LandingZoneDao;

@RestController
public class EtaxApi {
	@Autowired
	IdentifyInfoDao identifyInfoDao;

	@Autowired
	TaxPersonInfoDao taxPersonInfoDao;

	@Autowired
	EtaxDao etaxDao;

	@Autowired
	SettingDao settingDao;

	@Autowired
	MsbTokenDao msbTokenDao;

	@RequestMapping(value = "/api/etaxTransaction/readFileXml", method = RequestMethod.POST)
	@ResponseBody
	public Object readFileXml(@RequestParam("file") MultipartFile file, HttpServletRequest request,
			HttpServletResponse response) {

		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper().configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonResponeBase<?> jsonRes = null;

		try {
			String token = request.getHeader("token"); 
			Users user = msbTokenDao.getUserByToken(token);

			if (!file.isEmpty()) {
				String fileName = file.getOriginalFilename();
				Date date = new Date();
				SimpleDateFormat DateFor = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				String stringDate = DateFor.format(date);
				System.out.println(new Date());
				if ((fileName.toLowerCase().endsWith("xml")) == true
						|| (fileName.toLowerCase().endsWith("xml")) == true) {

					byte[] byteArray = file.getBytes();
					ByteArrayInputStream source = new ByteArrayInputStream(byteArray);
					InputSource input = new InputSource(source);
					ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
					CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
					input.setEncoding("UTF-8");

					JAXBContext jaxbContext = JAXBContext.newInstance(GIAODICH.class);
					Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
					// StringReader inPut = new StringReader(content1);
					GIAODICH customer = (GIAODICH) jaxbUnmarshaller.unmarshal(input);
					NDDSHDR nddshdr = customer.getNdDsNnt().getNdDsHdr();
					System.out.print(jaxbUnmarshaller);
					List<TaxPersonInfo> lstTaxPersonInfo = new ArrayList<TaxPersonInfo>();

					List<IdentifyInfo> lstGiayTo = new ArrayList<IdentifyInfo>();
					if (Utils.checkIsNullOrEmpty(nddshdr.getThangGui())) {
						String thangGui = Utils.getStringDate(date, "MM/YYYY").replaceFirst("^0", "");
						nddshdr.setThangGui(thangGui);
					}

					// không upload trùng filename + kỳ dữ liệu
				    JSONObject jsonObj = new JSONObject();
				    jsonObj.put("period", Utils.convertMMYYYYtoFormat(nddshdr.getThangGui(), "YYYY/MM"));
				    jsonObj.put("status", "0");
				    jsonObj.put("fileName", fileName);
			        String jsonString = jsonObj.toString();
					FileNameUploadXml search = mapper.readValue(jsonString, FileNameUploadXml.class);
					Long lstMaxRecord = (Long) etaxDao.countDupplicate(search, user);
					if (lstMaxRecord.intValue() > 0) {
						status.append("400");
						desc.append("Tên file trong kỳ này đã tồn tại.");
						jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
						return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
					}

					for (ROWCTIET item : customer.getNdDsNnt().getNdDsCTiet().getRowCTiet()) {
						TaxPersonInfo taxPersonInfo = new TaxPersonInfo();
						taxPersonInfo.setMst(item.getMst());
						taxPersonInfo.setTenNnt(item.getTenNnt());
						taxPersonInfo.setNgaySinh(item.getNgaySinh());
						taxPersonInfo.setLoaiNnt(item.getLoaiNnt());
						taxPersonInfo.settThaiMst(item.getTthaiMst());
						taxPersonInfo.setpBanTLieuXml(nddshdr.getpBanTLieuXml());
						taxPersonInfo.setThangGui(nddshdr.getThangGui());
						taxPersonInfo.setFileName(fileName);
						taxPersonInfo.setCreatedDate(new Date());
						taxPersonInfo.setCreatedBy(user.getUserId());
						lstTaxPersonInfo.add(taxPersonInfo);

						for (GIAYTO giayTo : item.getTtinGiayTo().getGiayTo()) {
							IdentifyInfo identifyInfo = new IdentifyInfo();
							identifyInfo.setIdPersonInfo(taxPersonInfo.getId());
							identifyInfo.setFileName(fileName);
							identifyInfo.setLanGui(nddshdr.getLanGui());
							identifyInfo.setMst(item.getMst());
							identifyInfo.setNgayGui(nddshdr.getNgayGui());
							identifyInfo.setLoaiGiayTo(giayTo.getLoaiGiayTo());
							identifyInfo.setSoGiayTo(giayTo.getSoGiayTo());
							identifyInfo.setTenLoaiGiayTo(giayTo.getTenLoaiGiayTo());
							identifyInfo.setThangGui(nddshdr.getThangGui());
							lstGiayTo.add(identifyInfo);
						}
					}
					List<FileNameUploadXml> lstFileNameUploadXml = new ArrayList<FileNameUploadXml>();
					FileNameUploadXml fileNameUploadXml = new FileNameUploadXml();
					fileNameUploadXml.setFileName(fileName);

					fileNameUploadXml.setPeriod(Utils.convertMMYYYYtoFormat(nddshdr.getThangGui(), "YYYY/MM"));

//					fileNameUploadXml.setPeriod("2024/04");
					fileNameUploadXml.setDescription("Upload vào ngày " + formatter.format(date));
					fileNameUploadXml.setNumberOfRecords(lstTaxPersonInfo.size());
					fileNameUploadXml.setCreateBy(user.getUserId());
					fileNameUploadXml.setCreateTime(new Date());
					fileNameUploadXml.setStatus("0");
					lstFileNameUploadXml.add(fileNameUploadXml);
					fileNameUploadXml.setContent(charBuffer.toString());
					
					List<PeriodUploadXml> lstPeriodUploadXml = new ArrayList<PeriodUploadXml>();
					PeriodUploadXml periodUploadXml = new PeriodUploadXml();
//					periodUploadXml.setId(fileNameUploadXml.getId());
					periodUploadXml.setPeriod(fileNameUploadXml.getPeriod());
					periodUploadXml.setStatus("0");
					lstPeriodUploadXml.add(periodUploadXml);
					taxPersonInfoDao.saveList(lstTaxPersonInfo);
					// taxPersonInfoDao.saveList(lstTaxPersonInfo);
					identifyInfoDao.saveList(lstGiayTo);
					etaxDao.saveList(lstFileNameUploadXml, lstPeriodUploadXml);
					System.out.println(new Date());

					status.append("200");
					desc.append("THÀNH CÔNG");
				} else {
					status.append("500");
					desc.append("Ngoai le 1");
					jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
					return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);

				}
			} else {
				status.append("500");
				desc.append("Ngoai le 1");
				jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
				return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
			}

		} catch (Exception e) {
			e.printStackTrace();
			status.append("500");
			desc.append(e.getMessage());
			jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
			return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
		}
		jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
		return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);

	}

	@RequestMapping(value = "/api/etaxTransaction/getListXmlFileName", method = RequestMethod.POST)
	@ResponseBody
	public Page<FileNameUploadXml> getListXmlFileName(@RequestBody String register, HttpServletRequest request) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		List<FileNameUploadXml> listResult = new ArrayList<FileNameUploadXml>();
		ObjectMapper mapper = new ObjectMapper().configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		int maxRecord = 0;
		try {
			String registerResp = java.net.URLDecoder.decode(register, "UTF-8");
			String jsonObj = Utils.parametersToJson(registerResp);
			FileNameUploadXml search = mapper.readValue(jsonObj, FileNameUploadXml.class);
			String token = request.getHeader("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {
				Long lstMaxRecord = (Long) etaxDao.countTotal(search, user);
				if (lstMaxRecord != null)
					maxRecord = lstMaxRecord.intValue();
				listResult = etaxDao.getListXmlFileName(search, maxRecord);
				return new Page<FileNameUploadXml>(listResult, search.getPage(), search.getRows() - 1, maxRecord);
			} else {
				status.append("500");
				desc.append("User khong hop le");
			}
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}
		return new Page<FileNameUploadXml>(listResult, 0, 0, maxRecord);
	}

	@RequestMapping(value = "/api/etaxTransaction/deleteXmlFileName", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity deleteXmlFileName(@RequestBody ListOfId listOfId, HttpServletRequest request) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		JsonResponeBase<?> jsonRes = null;
		FileNameUploadXml fileNameDelete = null;
		String token = request.getHeader("token");
		Users user = msbTokenDao.getUserByToken(token);
		int count = 0;
		int countError = 0;
		try {
			if (listOfId == null || listOfId.getIds() == null) {
				jsonRes = new JsonResponeBase<Object>("500", "Request data error!", null);
				return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
			}
			for (int i = 0; i < listOfId.getIds().size(); i++) {
				System.out.println("id...." + listOfId.getIds().get(i));
				fileNameDelete = etaxDao.getById(listOfId.getIds().get(i));
				if (fileNameDelete == null) {
					status.append("500");
					desc.append("Không tìm thấy thông tin!");
					countError++;
					jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
					return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
				}
				etaxDao.remove(fileNameDelete);
				count++;
			}
			status.append("200");
			desc.append("XÓA THÀNH CÔNG " + count + " bản ghi - Lỗi " + countError + " bản ghi");
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}
		jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
		return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
	}

	@RequestMapping(value = "/api/etaxTransaction/exportListXmlFileName", method = RequestMethod.GET)
	@ResponseBody
	public void exportListXmlFileName(@RequestParam("fileId") String fileId, Principal principal,
			RedirectAttributes redirect, HttpServletRequest request, HttpServletResponse response) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		JsonResponeBase<?> jsonRes = null;
		try {
			String token = request.getParameter("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {

				FileNameUploadXml entity = etaxDao.getById(Long.parseLong(fileId));

				String xmlContent = exportEntityToXml(entity);
				String xmlFileName = entity.getFileName().trim();
				
				response.setContentType("application/xml");
	            response.setHeader("Content-Disposition", "attachment; filename=" + xmlFileName);
	            response.setCharacterEncoding("UTF-8");
	            
	            try (PrintWriter writer = response.getWriter()) {
	                writer.write(xmlContent.trim());
	                writer.flush();
	            }
	            
			}
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}

	}

	@RequestMapping(value = "/api/etaxTransaction/downloadSampleXml", method = RequestMethod.GET)
	@ResponseBody
	public void downloadSampleXml(HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = null;
		try {
			String token = request.getParameter("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {
	            String realPath = request.getSession().getServletContext()
	                    .getRealPath("/WEB-INF/reports/appEtax/TEMPLATE.xml");
	            File templateFile = new File(realPath);
	            if (!templateFile.exists()) {
	                throw new FileNotFoundException("Tệp TEMPLATE.xml không tồn tại ở đường dẫn: " + realPath);
	            }
	            is = new FileInputStream(templateFile);
				response.setContentType("application/xml");
	            response.setHeader("Content-Disposition", "attachment; filename=File-gui-ous-dau-vao-dinh-ky-Samp.xml");
	            response.setCharacterEncoding("UTF-8");
				IOUtils.copy(is, response.getOutputStream());
				response.flushBuffer();
				is.close();
				return;
			}
	    } catch (FileNotFoundException e) {
	        response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
	    } catch (IOException e) {
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi xử lý tệp: " + e.getMessage());
	    } finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	}

	private String exportEntityToXml(FileNameUploadXml entity) throws Exception {
	    // Create a new DocumentBuilderFactory and DocumentBuilder
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();

	    // Create a new XML document
	    Document document = builder.newDocument();

	    Element root = document.createElement("FileNameUploadXml");
	    document.appendChild(root);

	    createElementWithTextContent(document, root, "Id", String.valueOf(entity.getId()));
	    createElementWithTextContent(document, root, "Period", entity.getPeriod());
	    createElementWithTextContent(document, root, "FileName", entity.getFileName());
	    createElementWithTextContent(document, root, "CreateBy", entity.getCreateBy());

	    String createTimeFormatted = entity.getCreateTime() != null ?
	        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(entity.getCreateTime()) : "";
	    createElementWithTextContent(document, root, "CreateTime", createTimeFormatted);
	    
	    createElementWithTextContent(document, root, "NumberOfRecords", String.valueOf(entity.getNumberOfRecords()));
	    createElementWithTextContent(document, root, "Description", entity.getDescription());
	    createElementWithTextContent(document, root, "Status", entity.getStatus());
	    
	    String cleanedContent = cleanXmlContent(entity.getContent());
        createElementWithCData(document, root, "Content", cleanedContent);

	    // Convert the document to a string
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    StringWriter writer = new StringWriter();
	    transformer.transform(new DOMSource(document), new StreamResult(writer));

	    return writer.getBuffer().toString();
	}

	@RequestMapping(value = "/api/etaxTransaction/lockPeriods", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity lockPeriods(@RequestParam("period") String period, HttpServletRequest request, HttpServletResponse response) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		PeriodUploadXml periodToDone = null;
		JsonResponeBase<?> jsonRes = null;
		String token = request.getHeader("token");
		int count = 0;
		int countError = 0;
		try {
			Users user = msbTokenDao.getUserByToken(token);
			if (user == null) {
	            jsonRes = new JsonResponeBase<Object>("401", "Unauthorized access!", null);
	            return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.UNAUTHORIZED);
			}
//			List<FileNameUploadXml> entity = etaxDao.getFileNamesByPeriod(period, 9999);
//			for (Long id : lstOfId) {
//				try {
//					if (entity == null) {
//						countError = countError + 1;
//						break;
//					}
//					entity.setStatus("3");
//					etaxDao.update(entity);
//					count++;
//				} catch (Exception ex) {
//					countError++;
//					ex.printStackTrace();
//				}
//			}
			status.append("200");
			desc.append("Chốt dữ liệu THÀNH CÔNG " + count + " bản ghi - Lỗi " + countError + " bản ghi");
	    } catch (Exception e) {
	        status.append("500");
	        desc.append(e.getMessage());
	        e.printStackTrace();
	    }
		jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
		return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/api/etaxTransaction/lockPeriodData", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity lockPeriodData(@RequestBody ListOfId ids, HttpServletRequest request, HttpServletResponse response) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		PeriodUploadXml periodToDone = null;
		List<Long> lstOfId = ids.getIds();
		JsonResponeBase<?> jsonRes = null;
		String token = request.getHeader("token");
		int count = 0;
		int countError = 0;
		try {
			Users user = msbTokenDao.getUserByToken(token);
			if (user == null) {
	            jsonRes = new JsonResponeBase<Object>("401", "Unauthorized access!", null);
	            return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.UNAUTHORIZED);
			}
			for (Long id : lstOfId) {
				try {
					FileNameUploadXml entity = etaxDao.getById(id);
					if (entity == null) {
						countError = countError + 1;
						break;
					}
					entity.setStatus("3");
					etaxDao.update(entity);
					count++;
				} catch (Exception ex) {
					countError++;
					ex.printStackTrace();
				}
			}
			status.append("200");
			desc.append("Chốt dữ liệu THÀNH CÔNG " + count + " bản ghi - Lỗi " + countError + " bản ghi");
	    } catch (Exception e) {
	        status.append("500");
	        desc.append(e.getMessage());
	        e.printStackTrace();
	    }
		jsonRes = new JsonResponeBase<Object>(status.toString(), desc.toString(), null);
		return new ResponseEntity<JsonResponeBase<?>>(jsonRes, HttpStatus.OK);
	}

	@RequestMapping(value = "/api/etaxTransaction/exportListXmlPeriod", method = RequestMethod.GET)
	@ResponseBody
	public void exportListXmlPeriod(@RequestParam("fileId") String fileId, Principal principal,
			RedirectAttributes redirect, HttpServletRequest request, HttpServletResponse response) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		JsonResponeBase<?> jsonRes = null;
		try {
			String token = request.getParameter("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {

				FileNameUploadXml entity = etaxDao.getById(Long.parseLong(fileId));

				String xmlContent = exportPeriod(entity);
				
				response.setContentType("application/xml");
	            response.setHeader("Content-Disposition", "attachment; filename=export_PERIOD.xml");
	            response.setCharacterEncoding("UTF-8");
	            
	            try (PrintWriter writer = response.getWriter()) {
	                writer.write(xmlContent.trim());
	                writer.flush();
	            }
	            
			}
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}

	}

	private String exportPeriod(FileNameUploadXml entity) throws Exception {
	    // Create a new DocumentBuilderFactory and DocumentBuilder
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();

	    // Create a new XML document
	    Document document = builder.newDocument();

	    Element root = document.createElement("HSoThueDTu");
	    root.setAttribute("xmlns", "http://kekhaithue.gdt.gov.vn/TKhaiThue");
	    root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	    document.appendChild(root);
	    
	    // Create and append the HSoKhaiThue element
	    Element hSoKhaiThue = document.createElement("HSoKhaiThue");
	    hSoKhaiThue.setAttribute("id", "_NODE_TO_SIGN");
	    root.appendChild(hSoKhaiThue);
	    
	    Element tTinChung = document.createElement("TTinChung");
	    hSoKhaiThue.appendChild(tTinChung);
	    
	    Element tTinDVu = document.createElement("TTinDVu");
	    tTinChung.appendChild(tTinDVu);
	    
	    createElementWithTextContent(document, tTinDVu, "maDVu", "ETAX");
	    createElementWithTextContent(document, tTinDVu, "tenDVu", "ETAX 1.0");
	    createElementWithTextContent(document, tTinDVu, "pbanDVu", "1.0");
	    createElementWithTextContent(document, tTinDVu, "ttinNhaCCapDVu", "ETAX_TCT");
	    
	    Element tTinTKhaiThue = document.createElement("TTinTKhaiThue");
	    tTinChung.appendChild(tTinTKhaiThue);
	    
	    Element tKhaiThue = document.createElement("TKhaiThue");
	    tTinTKhaiThue.appendChild(tKhaiThue);
	    
	    NDDSHDR nDDSHDR = new NDDSHDR();
	    IdentifyInfo identifyInfo = new IdentifyInfo();
	    TaxPersonInfo search = new TaxPersonInfo();
	    GIAYTO giayTo = new GIAYTO();
	    ROWCTIET rowChiTiet = new ROWCTIET();
	    
	    search.setId(entity.getId());
	    search.setFileName(entity.getFileName());
	    search.setCreatedDate(entity.getCreateTime());
	    search.setpBanTLieuXml(nDDSHDR.getpBanTLieuXml());
	    
	    createElementWithTextContent(document, tKhaiThue, "maTKhai", "717");
	    createElementWithTextContent(document, tKhaiThue, "tenTKhai", "Danh sách số hiệu tài khoản thanh toán (TT126/2020)");
	    createElementWithTextContent(document, tKhaiThue, "moTaBMau", "");
	    createElementWithTextContent(document, tKhaiThue, "pbanTKhaiXML", "4.0.1");
	    createElementWithTextContent(document, tKhaiThue, "loaiTKhai", "C");
	    createElementWithTextContent(document, tKhaiThue, "soLan", "1");
	    
	    Element kyKKhaiThue = document.createElement("KyKKhaiThue");
	    tKhaiThue.appendChild(kyKKhaiThue);

	    createElementWithTextContent(document, kyKKhaiThue, "kieuKy", identifyInfo.getLanGui());
	    createElementWithTextContent(document, kyKKhaiThue, "kyKKhai", "03/2022");
	    createElementWithTextContent(document, kyKKhaiThue, "kyKKhaiTuNgay", "01/03/2022");
	    createElementWithTextContent(document, kyKKhaiThue, "kyKKhaiDenNgay", "31/03/2022");
	    createElementWithTextContent(document, kyKKhaiThue, "kyKKhaiTuThang", "03/2022");
	    createElementWithTextContent(document, kyKKhaiThue, "kyKKhaiDenThang", "03/2022");
	    
	    createElementWithTextContent(document, tKhaiThue, "maCQTNoiNop", "10100");
	    createElementWithTextContent(document, tKhaiThue, "tenCQTNoiNop", "Cục Thuế Thành phố Hà Nội");
	    createElementWithTextContent(document, tKhaiThue, "ngayLapTKhai", "27/04/2022");
	    createElementWithTextContent(document, tKhaiThue, "nguoiKy", "");
	    createElementWithTextContent(document, tKhaiThue, "ngayKy", "2022-04-27");
	    createElementWithTextContent(document, tKhaiThue, "nganhNgheKD", "");
	    
	    // Create NNT element
	    Element nnt = document.createElement("NNT");
	    tTinTKhaiThue.appendChild(nnt);
	    
	    identifyInfo.setFileName(entity.getFileName());
	    List<TaxPersonInfo> lst = etaxDao.getTaxPersonInfo(search, 20000);
	    	for(TaxPersonInfo taxPersonInfo : lst) {
	    		createElementWithTextContent(document, nnt, "mst", taxPersonInfo.getMst());
			    createElementWithTextContent(document, nnt, "tenNNT", taxPersonInfo.getTenNnt());
			    createElementWithTextContent(document, nnt, "dchiNNT", "98A, Nguy Nhu, Kontum");
			    createElementWithTextContent(document, nnt, "phuongXa", "");
			    createElementWithTextContent(document, nnt, "maHuyenNNT", "10101");
			    createElementWithTextContent(document, nnt, "tenHuyenNNT", "Thành phố Cao Bằng");
			    createElementWithTextContent(document, nnt, "maTinhNNT", "101");
			    createElementWithTextContent(document, nnt, "tenTinhNNT", "Thành Phố Hà Nội");
			    createElementWithTextContent(document, nnt, "dthoaiNNT", "0914428088");
			    createElementWithTextContent(document, nnt, "faxNNT", "");
			    createElementWithTextContent(document, nnt, "emailNNT", "niunt@seatechit.com.vn");
	    	}
	    	
	    
	    
	    Element cTieuTKhaiChinh = document.createElement("CTieuTKhaiChinh");
	    hSoKhaiThue.appendChild(cTieuTKhaiChinh);
	    
	    Element nOIDUNG_DANHSACH_HDR = document.createElement("NOIDUNG_DANHSACH_HDR");
	    cTieuTKhaiChinh.appendChild(nOIDUNG_DANHSACH_HDR);

	    createElementWithTextContent(document, nOIDUNG_DANHSACH_HDR, "MA_NH", "01201001");
	    createElementWithTextContent2(document, nOIDUNG_DANHSACH_HDR, "TONGSO_BANGHI", entity.getNumberOfRecords());
	    
	    Element nOIDUNG_DANHSACH_CTIET = document.createElement("NOIDUNG_DANHSACH_CTIET");
	    cTieuTKhaiChinh.appendChild(nOIDUNG_DANHSACH_CTIET);
	    
	    Element rOW_CTIET = document.createElement("ROW_CTIET");
        nOIDUNG_DANHSACH_CTIET.appendChild(rOW_CTIET);
        List<IdentifyInfo> lstGiayTo = etaxDao.getIdentifyInfo(identifyInfo, 20000);
	    for(IdentifyInfo idInfo : lstGiayTo) { // Example for 5 rows, adjust as necessary

	        for(int i=0;i<entity.getNumberOfRecords();i++){
	        	createElementWithTextContent(document, rOW_CTIET, "STT", String.valueOf(i + 1));
	        
	        	createElementWithTextContent(document, rOW_CTIET, "MST", "");
		        Element tTIN_GIAYTO = document.createElement("TTIN_GIAYTO");
		        rOW_CTIET.appendChild(tTIN_GIAYTO);

		        Element gIAYTO = document.createElement("GIAYTO");
		        tTIN_GIAYTO.appendChild(gIAYTO);

		        createElementWithTextContent(document, gIAYTO, "LOAI_GIAYTO", idInfo.getLoaiGiayTo());
		        createElementWithTextContent(document, gIAYTO, "SO_GIAYTO", idInfo.getSoGiayTo());

		        createElementWithTextContent(document, rOW_CTIET, "TEN_CHUTK", rowChiTiet.getTenNnt());
		        createElementWithTextContent(document, rOW_CTIET, "SOHIEU_TK", "SampleContent" + i);
		        createElementWithTextContent(document, rOW_CTIET, "NGAY_MO", "07/04/2010");
		        createElementWithTextContent(document, rOW_CTIET, "NGAY_DONG", "26/04/2022");
		        createElementWithTextContent(document, rOW_CTIET, "LOAI_KH", rowChiTiet.getLoaiNnt());
	        }
	        
	    }

	    // Convert the document to a string
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    StringWriter writer = new StringWriter();
	    transformer.transform(new DOMSource(document), new StreamResult(writer));

	    return writer.toString();
	}

	private static void createElementWithTextContent(Document document, Element parent, String tagName, String textContent) {
	    Element element = document.createElement(tagName);
	    element.appendChild(document.createTextNode(textContent != null ? textContent : ""));
	    parent.appendChild(element);
	}
	
	private static void createElementWithTextContent2(Document document, Element parent, String tagName, Integer textContent) {
		Element element = document.createElement(tagName);
	    // Convert Integer to String and handle null case
	    String text = (textContent != null) ? textContent.toString() : "";
	    element.appendChild(document.createTextNode(text));
	    parent.appendChild(element);
	}
	
	private static void createElementWithCData(Document document, Element parent, String tagName, String textContent) {
	    Element element = document.createElement(tagName);
	    if (textContent != null) {
	        CDATASection cdata = document.createCDATASection(textContent);
	        element.appendChild(cdata);
	    }
	    parent.appendChild(element);
	}
	
	private static String cleanXmlContent(String content) {
	    if (content == null) {
	        return "";
	    }
	    // Remove extra blank lines and trim spaces
	    return content.replaceAll("(?m)^[\\s]*\r?\n", "").trim();
	}

    private String convertPeriodFormat(String period) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat("MM-yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM/yyyy");
        Date date = inputFormat.parse(period);
        return outputFormat.format(date);
    }

	@RequestMapping(value = "/api/etaxTransaction/getListDownloadXmlFileName", method = RequestMethod.POST)
	@ResponseBody
	public Page<XmlTaxExptFile> getListDownloadXmlFileName(@RequestBody String register, HttpServletRequest request) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		List<XmlTaxExptFile> listResult = new ArrayList<XmlTaxExptFile>();
		List<Object> listParam = new ArrayList<Object>();
		ObjectMapper mapper = new ObjectMapper().configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		int maxRecord = 0;
		try {
			String encodedRegister = URLEncoder.encode(register, "UTF-8");
            String registerResp = URLDecoder.decode(encodedRegister, "UTF-8");
			String jsonObj = Utils.parametersToJson(registerResp);
			XmlTaxExptFileFilterReq search = mapper.readValue(jsonObj, XmlTaxExptFileFilterReq.class);
			String token = request.getHeader("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {
				List<Setting> lstSetting = settingDao.getListByType("TAX");

				Setting driverDBTax = getSettingByName("TAX_ORA_DRIVER", lstSetting);
				Setting hostFBTax = getSettingByName("TAX_ORA_HOST", lstSetting);
				Setting userDBTax = getSettingByName("TAX_ORA_PASS", lstSetting);
				Setting passFBTax = getSettingByName("TAX_ORA_USER", lstSetting);
				LandingZoneDao landingzone = new LandingZoneDao();
				
				System.out.println("paramater: " + driverDBTax + "|" + hostFBTax + "|" + userDBTax + "|" + passFBTax);

				Connection connection = null;
				PreparedStatement stmt = null; 
				PreparedStatement stmtCount = null; 

				try {
					connection = landingzone.LandingZoneConnect(
						driverDBTax.getValue().trim(), 
						hostFBTax.getValue().trim(), 
						userDBTax.getValue().trim(), 
						passFBTax.getValue().trim()
					);
//					String sql1 = "select NUM_FILE, KY_GUI, CREATED_DATE, LOAI_KH from XML_TAX_EXPT_FILE a where 1=1 ";
					String sql1 = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM NUM_FILE, KY_GUI, CREATED_DATE, LOAI_KH WHERE 1=1 ";
			        StringBuilder sql = new StringBuilder("SELECT * FROM (SELECT a.NUM_FILE, a.KY_GUI, a.CREATED_DATE, a.LOAI_KH, ROWNUM rnum FROM (SELECT * FROM XML_TAX_EXPT_FILE WHERE 1=1 ");

			        StringBuilder sqlCount = new StringBuilder("select count(a.NUM_FILE) from XML_TAX_EXPT_FILE a where 1=1 ");

					if (!Utils.checkIsNullOrEmpty(search.getPeriod())) {
						sql.append(" and KY_GUI = ? ");
						sqlCount.append(" and a.KY_GUI = ? ");
			            String period = convertPeriodFormat(search.getPeriod().trim());
						listParam.add(period);
					}

					if (!Utils.checkIsNullOrEmpty(search.getFileName())) {
						sql.append(" and NUM_FILE like ? ");
						sqlCount.append(" and a.NUM_FILE like ? ");
						listParam.add("%" + search.getFileName().trim() + "%");
					}

					if (!Utils.checkIsNullOrEmpty(search.getCustomerType()) && !search.getCustomerType().equals("-1")) {
						sql.append(" and LOAI_KH = ? ");
						sqlCount.append(" and a.LOAI_KH = ? ");
						listParam.add(search.getCustomerType().trim());
					}

					stmtCount = connection.prepareStatement(sqlCount.toString());
					for (int i = 0; i < listParam.size(); i++) {
						stmtCount.setObject(i + 1, listParam.get(i));
					}
					ResultSet rsCount = stmtCount.executeQuery();
				    rsCount.next();
				    maxRecord = rsCount.getInt(1);

					final int startIdx = (search.getPage() - 1) * search.getRows();
					final int endIdx = Math.min(startIdx + search.getRows(), maxRecord);

				    sql.append(" ORDER BY KY_GUI DESC) a WHERE ROWNUM <= ?) WHERE rnum > ?");
			        listParam.add(endIdx);
			        listParam.add(startIdx);

					stmt = connection.prepareStatement(sql.toString());
					for (int i = 0; i < listParam.size(); i++) {
					    stmt.setObject(i + 1, listParam.get(i));
					}
					stmt.setMaxRows(search.getRows());

					ResultSet rs = stmt.executeQuery();

					 while (rs.next()) {
						XmlTaxExptFile xmlTaxExptFile = new XmlTaxExptFile();

//                        String value = rs.getString("KQ");
//                        xmlTaxExptFile.setContent(value);

						String value = rs.getString("NUM_FILE");
                        xmlTaxExptFile.setFileName(value);

                        value = rs.getString("KY_GUI");
                        xmlTaxExptFile.setPeriod(value);

                        Timestamp timestamp = rs.getTimestamp("CREATED_DATE");
                        if (timestamp != null) {
                            xmlTaxExptFile.setCreateTime(new Date(timestamp.getTime()));
                        }

                        value = rs.getString("LOAI_KH");
                        xmlTaxExptFile.setCustomerType(value);

                        listResult.add(xmlTaxExptFile);
					 }

					return new Page<XmlTaxExptFile>(listResult, search.getPage(), search.getRows() - 1, maxRecord);
				} catch (Exception e) {
					System.out.println("getListBdsSignImage error: " + e.getMessage());
					e.printStackTrace();
				} finally {
					if (stmt != null) {
						try {
							stmt.close();
						} catch (SQLException ex) {
							System.out.println("Close PreparedStatement sttm: ERR " + ex.getMessage());

						}
					}

					try {
						if (connection != null && !connection.isClosed()) {    
							connection.close();
						}
					} catch (SQLException ex) {
						System.out.println("Close Connection connection: ERR " + ex.getMessage());

					}

				}

			} else {
				status.append("500");
				desc.append("User khong hop le");
			}
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}
		return new Page<XmlTaxExptFile>(listResult, 0, 0, maxRecord);
	}

	@RequestMapping(value = "/api/etaxTransaction/exportDownloadXmlFileName", method = RequestMethod.GET)
	@ResponseBody
	public void exportDownloadXmlFileName(@RequestParam("fileId") String fileId, Principal principal,
			RedirectAttributes redirect, HttpServletRequest request, HttpServletResponse response) {
		StringBuilder status = new StringBuilder();
		StringBuilder desc = new StringBuilder();
		JsonResponeBase<?> jsonRes = null;
		try {
			String token = request.getParameter("token");
			Users user = msbTokenDao.getUserByToken(token);
			if (user != null) {
				List<Setting> lstSetting = settingDao.getListByType("TAX");

				Setting driverDBTax = getSettingByName("TAX_ORA_DRIVER", lstSetting); 
				Setting hostFBTax = getSettingByName("TAX_ORA_HOST", lstSetting);
				Setting userDBTax = getSettingByName("TAX_ORA_PASS", lstSetting);
				Setting passFBTax = getSettingByName("TAX_ORA_USER", lstSetting); 
				LandingZoneDao landingzone = new LandingZoneDao();
				
				System.out.println("paramater: " + driverDBTax + "|" + hostFBTax + "|" + userDBTax + "|" + passFBTax);

				Connection connection = null;
				PreparedStatement stmt = null; 

				try {
					connection = landingzone.LandingZoneConnect(
						driverDBTax.getValue().trim(), 
						hostFBTax.getValue().trim(), 
						userDBTax.getValue().trim(), 
						passFBTax.getValue().trim()
					);

					String sql = "select KQ, NUM_FILE from XML_TAX_EXPT_FILE a where 1=1 AND a.NUM_FILE = ? ";

					stmt = connection.prepareStatement(sql);
					stmt.setObject(1, fileId);
					ResultSet rs = stmt.executeQuery();
				    rs.next();

		            String xmlContent = rs.getString(1);
		            String xmlFileName = rs.getString(2).trim();
				    
					response.setContentType("application/xml");
		            response.setHeader("Content-Disposition", "attachment; filename=" + xmlFileName + ".xml");
		            response.setCharacterEncoding("UTF-8");

		            try (PrintWriter writer = response.getWriter()) { 
		                writer.write(xmlContent.trim());
		                writer.flush();
		            }
	            } catch (Exception e) {
					System.out.println("getListBdsSignImage error: " + e.getMessage());
					e.printStackTrace();
				} finally {
					if (stmt != null) {
						try {
							stmt.close();
						} catch (SQLException ex) {
							System.out.println("Close PreparedStatement sttm: ERR " + ex.getMessage());

						}
					}

					try {
						if (connection != null && !connection.isClosed()) {    
							connection.close();
						}
					} catch (SQLException ex) {
						System.out.println("Close Connection connection: ERR " + ex.getMessage());

					}

				}
	            
			}
		} catch (Exception e) {
			status.append("500");
			desc.append(e.getMessage());
			e.printStackTrace();
		}

	}

	public static Setting getSettingByName(String name, List<Setting> lst) {
		for (int i = 0; i < lst.size(); i++) {
			Setting item = lst.get(i);
			if (item.getName().equals(name))
				return item;
		}
		return null;

	}

}
