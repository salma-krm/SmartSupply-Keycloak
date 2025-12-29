package org.smartsupply.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.SupplierRequestDto;
import org.smartsupply.dto.request.SupplierUpdateDto;
import org.smartsupply.dto.response.SupplierResponseDto;
import org.smartsupply.dto.response.SupplierSimpleDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.SupplierMapper;
import org.smartsupply.model.entity.Supplier;
import org.smartsupply.repository.PurchaseOrderRepository;
import org.smartsupply.repository.SupplierRepository;
import org.smartsupply.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupplierServiceImp implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    @Transactional
    public SupplierResponseDto createSupplier(SupplierRequestDto dto) {
        log.info("Create supplier name={}", dto.getName());
        if (supplierRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Un fournisseur avec cet email existe déjà");
        }
        if (supplierRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Un fournisseur avec ce nom existe déjà");
        }
        Supplier s = supplierMapper.toEntity(dto);
        Supplier saved = supplierRepository.save(s);
        return supplierMapper.toResponseDto(saved);
    }

    @Override
    public List<SupplierResponseDto> getAllSuppliers() {
        return supplierMapper.toResponseDtoList(supplierRepository.findAll());
    }

    @Override
    public SupplierResponseDto getSupplierById(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable id=" + id));
        return supplierMapper.toResponseDto(s);
    }

    @Override
    @Transactional
    public SupplierResponseDto updateSupplier(Long id, SupplierUpdateDto dto) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable id=" + id));

        if (!s.getEmail().equals(dto.getEmail()) && supplierRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Un fournisseur avec cet email existe déjà");
        }
        if (!s.getName().equals(dto.getName()) && supplierRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Un fournisseur avec ce nom existe déjà");
        }

        supplierMapper.updateEntityFromDto(dto, s);
        Supplier updated = supplierRepository.save(s);
        return supplierMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable id=" + id));

        long poCount = purchaseOrderRepository.countBySupplierId(id);
        if (poCount > 0) {
            throw new BusinessException("Impossible de supprimer le fournisseur car " + poCount + " PurchaseOrder(s) le référencent");
        }

        supplierRepository.delete(s);
        log.info("Supplier deleted id={}", id);
    }

    @Override
    public List<SupplierSimpleDto> getAllSimple() {
        return supplierRepository.findAll().stream().map(supplierMapper::toSimpleDto).toList();
    }

    @Override
    public List<SupplierResponseDto> search(String q) {
        return supplierRepository.findByNameContainingIgnoreCase(q).stream().map(supplierMapper::toResponseDto).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return supplierRepository.existsById(id);
    }
}