package vn.com.m1tech.cnn.appEtax.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import vn.com.m1tech.cnn.appEtax.entities.IdentifyInfo;
import vn.com.m1tech.cnn.dao.GenericDao;

@Repository("identifyInfoDao")
public interface IdentifyInfoDao extends GenericDao<IdentifyInfo, Long> {

	public boolean saveList(List<IdentifyInfo> lstLog);

}
