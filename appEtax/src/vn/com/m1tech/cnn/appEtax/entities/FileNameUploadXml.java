package vn.com.m1tech.cnn.appEtax.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import vn.com.m1tech.app.lib.common.JqGridParameters;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "MST_FILENAME")
@XmlRootElement
public class FileNameUploadXml extends JqGridParameters implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Basic(optional = false)
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MST_FILENAME")
	@SequenceGenerator(name = "SEQ_MST_FILENAME", sequenceName = "SEQ_MST_FILENAME", allocationSize = 1) 
	private Long id;

	@Column(name = "PERIOD")
	private String period;

	@Column(name = "FILE_NAME")
	private String fileName;

	@Column(name = "CREATE_BY")
	private String createBy;

	@Column(name = "CREATE_TIME")
	private Date createTime;

	@Column(name = "NUMBER_OF_RECORD")
	private Integer numberOfRecords;

	@Column(name = "DESCRIPTION")
	private String description;

	@Column(name = "STATUS")
	private String status;

	@Column(name = "CONTENT")
	private String content;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Integer getNumberOfRecords() {
		return numberOfRecords;
	}

	public void setNumberOfRecords(Integer numberOfRecords) {
		this.numberOfRecords = numberOfRecords;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
