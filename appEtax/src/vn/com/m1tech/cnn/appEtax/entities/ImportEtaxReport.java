package vn.com.m1tech.cnn.appEtax.entities;

import java.util.List;

public class ImportEtaxReport {

    String fromDate;

    String toDate;

    String makeBy;

    String checkBy;

    String dateExport;

    List<FileNameUploadXml> listItems;

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getMakeBy() {
        return makeBy;
    }

    public void setMakeBy(String makeBy) {
        this.makeBy = makeBy;
    }

    public String getCheckBy() {
        return checkBy;
    }

    public void setCheckBy(String checkBy) {
        this.checkBy = checkBy;
    }

    public String getDateExport() {
        return dateExport;
    }

    public void setDateExport(String dateExport) {
        this.dateExport = dateExport;
    }

    public List<FileNameUploadXml> getListItems() {
        return listItems;
    }

    public void setListItems(List<FileNameUploadXml> listItems) {
        this.listItems = listItems;
    }

}