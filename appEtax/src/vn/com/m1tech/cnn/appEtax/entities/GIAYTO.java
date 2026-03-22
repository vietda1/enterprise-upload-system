package vn.com.m1tech.cnn.appEtax.entities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GIAYTO {

	@XmlElement(name = "LOAI_GIAYTO", required = false)
	private String loaiGiayTo;

	@XmlElement(name = "TEN_LOAI_GIAYTO", required = false)
	private String tenLoaiGiayTo;

	@XmlElement(name = "SO_GIAYTO", required = false)
	private String soGiayTo;

	public String getLoaiGiayTo() {
		return loaiGiayTo;
	}

	public void setLoaiGiayTo(String loaiGiayTo) {
		this.loaiGiayTo = loaiGiayTo;
	}

	public String getTenLoaiGiayTo() {
		return tenLoaiGiayTo;
	}

	public void setTenLoaiGiayTo(String tenLoaiGiayTo) {
		this.tenLoaiGiayTo = tenLoaiGiayTo;
	}

	public String getSoGiayTo() {
		return soGiayTo;
	}

	public void setSoGiayTo(String soGiayTo) {
		this.soGiayTo = soGiayTo;
	}

}
