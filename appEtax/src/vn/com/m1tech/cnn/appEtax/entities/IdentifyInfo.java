package vn.com.m1tech.cnn.appEtax.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "MST_TTIN_GIAYTO")
public class IdentifyInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Basic(optional = false)
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TTGT_INFO")
	@SequenceGenerator(name = "SEQ_TTGT_INFO", sequenceName = "SEQ_TTGT_INFO", allocationSize = 1000) 
	private long id;

	@Column(name = "SO_GIAYTO")
	private String soGiayTo;

	@Column(name = "LOAI_GIAYTO")
	private String loaiGiayTo;

	@Column(name = "TEN_LOAI_GIAYTO")
	private String tenLoaiGiayTo;

	@Column(name = "MST")
	private String mst;

	@Column(name = "THANG_GUI")
	private String thangGui;

	@Column(name = "NGAY_GUI")
	private String ngayGui;

	@Column(name = "LAN_GUI")
	private String lanGui;

	@Column(name = "FILE_NAME")
	private String fileName;

	@Column(name = "ID_PERSON_INFO")
	private Long idPersonInfo;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSoGiayTo() {
		return soGiayTo;
	}

	public void setSoGiayTo(String soGiayTo) {
		this.soGiayTo = soGiayTo;
	}

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

	public String getMst() {
		return mst;
	}

	public void setMst(String mst) {
		this.mst = mst;
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

	public String getLanGui() {
		return lanGui;
	}

	public void setLanGui(String lanGui) {
		this.lanGui = lanGui;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getIdPersonInfo() {
		return idPersonInfo;
	}

	public void setIdPersonInfo(Long idPersonInfo) {
		this.idPersonInfo = idPersonInfo;
	}

}
