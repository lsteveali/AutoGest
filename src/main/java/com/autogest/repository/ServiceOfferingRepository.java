package com.autogest.repository;

import com.autogest.dao.ServiceOfferingDao;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;

import java.util.List;

public class ServiceOfferingRepository {

    private final ServiceOfferingDao dao;

    public ServiceOfferingRepository(ServiceOfferingDao dao) {
        this.dao = dao;
    }

    public ServiceOffering create(ServiceOffering entity) {
        return dao.create(entity);
    }

    public ServiceOffering findById(int id) {
        return dao.findById(id);
    }

    public List<ServiceOffering> findAll() {
        return dao.findAll();
    }

    public boolean update(ServiceOffering entity) {
        return dao.update(entity);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public List<ServiceOffering> search(String text, ServiceOfferingStatus status, String category) {
        return dao.search(text, status, category);
    }

    public List<String> findCategories() {
        return dao.findCategories();
    }

    public boolean existsByCode(String code) {
        return dao.existsByCode(code);
    }

    public boolean existsByCodeExcluding(String code, int id) {
        return dao.existsByCodeExcluding(code, id);
    }
}
