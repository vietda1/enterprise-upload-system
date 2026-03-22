package vn.com.m1tech.cnn.appEtax.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class NDDSHDR {

	@XmlElement(name = "PBAN_TLIEU_XML", required = false)
	private String pBanTLieuXml;

	@XmlElement(name = "LAN_GUI", required = false)
	private String lanGui;

	@XmlElement(name = "THANG_GUI", required = false)
	private String thangGui;

	@XmlElement(name = "NGAY_GUI", required = false)
	private String ngayGui;

	@XmlElement(name = "SO_GOI", required = false)
	private String soGoi;

	public String getpBanTLieuXml() {
		return pBanTLieuXml;
	}

	public void setpBanTLieuXml(String pBanTLieuXml) {
		this.pBanTLieuXml = pBanTLieuXml;
	}

	public String getLanGui() {
		return lanGui;
	}

	public void setLanGui(String lanGui) {
		this.lanGui = lanGui;
	}

	public String getThangGui() {
		return thangGui;
	}

	public void setThangGui(String thangGui) {
		this.thangGui = thangGui;
	}

	public String getNgayGui() {
		return ngayGui;
	}

	public void setNgayGui(String ngayGui) {
		this.ngayGui = ngayGui;
	}

	public String getSoGoi() {
		return soGoi;
	}

	public void setSoGoi(String soGoi) {
		this.soGoi = soGoi;
	}

}
