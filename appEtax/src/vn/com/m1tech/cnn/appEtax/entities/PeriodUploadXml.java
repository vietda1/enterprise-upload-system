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
import vn.com.m1tech.cnn.appEtax.entities.FileNameUploadXml;

import java.io.Serializable;

@Entity
@Table(name = "MST_FILENAME_PERIOD")
@XmlRootElement
public class PeriodUploadXml extends JqGridParameters implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Basic(optional = false)
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MST_PERIOD")
	@SequenceGenerator(name = "SEQ_MST_PERIOD", sequenceName = "SEQ_MST_PERIOD", allocationSize = 1) 
	private Long id;

	@Column(name = "PERIOD")
	private String period;

	@Column(name = "STATUS")
	private String status;

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


	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}