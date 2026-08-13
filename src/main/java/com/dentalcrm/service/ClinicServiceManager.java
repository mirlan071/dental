package com.dentalcrm.service;
import com.dentalcrm.common.NotFoundException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List; import static com.dentalcrm.service.ServiceDtos.*;
@Service @Transactional public class ClinicServiceManager {private final ClinicServiceRepository repo;public ClinicServiceManager(ClinicServiceRepository r){repo=r;}
 public ServiceResponse create(ServiceRequest r){ClinicService s=new ClinicService();s.setActive(r.active()==null||r.active());apply(s,r);return map(repo.save(s));}
 public ServiceResponse update(Long id,ServiceRequest r){ClinicService s=getForUpdate(id);apply(s,r);return map(s);}
 @Transactional(readOnly=true) public List<ServiceResponse> all(boolean activeOnly){return (activeOnly?repo.findByActiveTrueOrderByName():repo.findAll()).stream().map(this::map).toList();}
 @Transactional(readOnly=true) public ServiceResponse find(Long id){return map(get(id));}
 public ClinicService get(Long id){return repo.findById(id).orElseThrow(()->new NotFoundException("Услуга не найдена: "+id));}
 public ClinicService getForUpdate(Long id){return repo.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Услуга не найдена: "+id));}
 private void apply(ClinicService s,ServiceRequest r){s.setName(r.name().trim());s.setPrice(r.price());s.setDurationMinutes(r.durationMinutes());if(r.active()!=null)s.setActive(r.active());}
 private ServiceResponse map(ClinicService s){return new ServiceResponse(s.getId(),s.getName(),s.getPrice(),s.getDurationMinutes(),s.isActive());}
}
