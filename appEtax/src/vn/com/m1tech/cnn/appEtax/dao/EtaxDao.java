/**
 * 
 */
package vn.com.m1tech.cnn.appEtax.dao;

import org.springframework.stereotype.Repository;

import vn.com.m1tech.cnn.appEtax.entities.FileNameUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.IdentifyInfo;
import vn.com.m1tech.cnn.appEtax.entities.PeriodUploadXml;
import vn.com.m1tech.cnn.appEtax.entities.TaxPersonInfo;
import vn.com.m1tech.cnn.dao.GenericDao;
import vn.com.m1tech.cnn.domain.admin.Users;

import java.util.List;

@Repository("etaxDao")
public interface EtaxDao extends GenericDao<FileNameUploadXml, Long> {

	public Object countTotal(FileNameUploadXml search, Users user);

	public Object countDupplicate(FileNameUploadXml search, Users user);
	
	public List<FileNameUploadXml> getListXmlFileName(FileNameUploadXml search, int maxRecord);

	public boolean saveList(List<FileNameUploadXml> lstLog, List<PeriodUploadXml> lstPeriod);
	
	public FileNameUploadXml getById(Long id);
	
	public PeriodUploadXml getByPeriod(String period);
	
	public List<FileNameUploadXml> getFileNamesByPeriod(FileNameUploadXml search, int maxRecord);
	
	public List<TaxPersonInfo> getTaxPersonInfo(TaxPersonInfo search, int maxRecord);
	
	public List<IdentifyInfo> getIdentifyInfo(IdentifyInfo search, int maxRecord);
}
