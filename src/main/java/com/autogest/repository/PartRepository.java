package com.autogest.repository;

import com.autogest.dao.PartDao;
import com.autogest.model.Part;
import com.autogest.model.StockStatus;

import java.util.List;

public class PartRepository {

    private final PartDao dao;

    public PartRepository(PartDao dao) {
        this.dao = dao;
    }

    public Part create(Part entity) {
        return dao.create(entity);
    }

    public Part findById(int id) {
        return dao.findById(id);
    }

    public List<Part> findAll() {
        return dao.findAll();
    }

    public boolean update(Part entity) {
        return dao.update(entity);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public List<Part> search(String text, StockStatus status, String category) {
        return dao.search(text, status, category);
    }

    public boolean existsByCode(String code) {
        return dao.existsByCode(code);
    }

    public boolean existsByCodeExcluding(String code, int id) {
        return dao.existsByCodeExcluding(code, id);
    }

    public int countLowStock() {
        return dao.countLowStock();
    }

    public int countOutOfStock() {
        return dao.countOutOfStock();
    }
}
