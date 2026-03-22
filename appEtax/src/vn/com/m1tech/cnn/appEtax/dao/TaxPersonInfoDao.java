package vn.com.m1tech.cnn.appEtax.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import vn.com.m1tech.cnn.appEtax.entities.TaxPersonInfo;
import vn.com.m1tech.cnn.dao.GenericDao;

@Repository("taxPersonInfoDao")
public interface TaxPersonInfoDao extends GenericDao<TaxPersonInfo, Long> { 

	public boolean saveList(List<TaxPersonInfo> lstLog);

}
