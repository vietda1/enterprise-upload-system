package vn.com.m1tech.cnn.appEtax.entities;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class NDDSCTIET {

	@XmlElement(name = "ROW_CTIET", required = false)
	private List<ROWCTIET> rowCTiet;

	public List<ROWCTIET> getRowCTiet() {
		return rowCTiet;
	}

	public void setRowCTiet(List<ROWCTIET> rowCTiet) {
		this.rowCTiet = rowCTiet;
	}
 
}
