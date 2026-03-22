package vn.com.m1tech.cnn.appEtax.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import vn.com.m1tech.app.lib.common.JqGridParameters;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "MST_TTIN_NGUOI_NOPTHUE")
public class TaxPersonInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Basic(optional = false)
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TAX_PERSON_INFO")
	@SequenceGenerator(name = "SEQ_TAX_PERSON_INFO", sequenceName = "SEQ_TAX_PERSON_INFO", allocationSize = 1000)
	// @GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;

	@Column(name = "MST")
	private String mst;

	@Column(name = "TEN_NNT")
	private String tenNnt;

	@Column(name = "NGAY_SINH")
	private String ngaySinh;

	@Column(name = "LOAI_NNT")
	private String loaiNnt;

	@Column(name = "TTHAI_MST")
	private String tThaiMst;

	@Column(name = "PBAN_TLIEU_XML")
	private String pBanTLieuXml;

	@Column(name = "THANG_GUI")
	private String thangGui;

	@Column(name = "FILE_NAME")
	private String fileName;

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private Date createdDate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getLoaiNnt() {
		return loaiNnt;
	}

	public void setLoaiNnt(String loaiNnt) {
		this.loaiNnt = loaiNnt;
	}

	public String gettThaiMst() {
		return tThaiMst;
	}

	public void settThaiMst(String tThaiMst) {
		this.tThaiMst = tThaiMst;
	}

	public String getpBanTLieuXml() {
		return pBanTLieuXml;
	}

	public void setpBanTLieuXml(String pBanTLieuXml) {
		this.pBanTLieuXml = pBanTLieuXml;
	}

	public String getThangGui() {
		return thangGui;
	}

	public void setThangGui(String thangGui) {
		this.thangGui = thangGui;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

}
