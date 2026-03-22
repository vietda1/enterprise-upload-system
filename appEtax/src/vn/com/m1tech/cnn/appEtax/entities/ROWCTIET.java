package vn.com.m1tech.cnn.appEtax.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ROWCTIET {

	@XmlElement(name = "STT", required = false)
	private String stt;

	@XmlElement(name = "MST", required = false)
	private String mst;

	@XmlElement(name = "TEN_NNT", required = false)
	private String tenNnt;

	@XmlElement(name = "NGAY_SINH", required = false)
	private String ngaySinh;

	@XmlElement(name = "TTIN_GIAYTO", required = false)
	private TTINGIAYTO ttinGiayTo;

	@XmlElement(name = "LOAI_NNT", required = false)
	private String loaiNnt;

	@XmlElement(name = "TTHAI_MST", required = false)
	private String tthaiMst;

	public String getStt() {
		return stt;
	}

	public void setStt(String stt) {
		this.stt = stt;
	}

	public String getMst() {
		return mst;
	}

	public void setMst(String mst) {
		this.mst = mst;
	}

	public String getTenNnt() {
		return tenNnt;
	}

	public void setTenNnt(String tenNnt) {
		this.tenNnt = tenNnt;
	}

	public String getNgaySinh() {
		return ngaySinh;
	}

	public void setNgaySinh(String ngaySinh) {
		this.ngaySinh = ngaySinh;
	}

	public TTINGIAYTO getTtinGiayTo() {
		return ttinGiayTo;
	}

	public void setTtinGiayTo(TTINGIAYTO ttinGiayTo) { 
		this.ttinGiayTo = ttinGiayTo;
	}

	public String getLoaiNnt() {
		return loaiNnt;
	}

	public void setLoaiNnt(String loaiNnt) {
		this.loaiNnt = loaiNnt;
	}

	public String getTthaiMst() {
		return tthaiMst;
	}

	public void setTthaiMst(String tthaiMst) {
		this.tthaiMst = tthaiMst;
	}

}
