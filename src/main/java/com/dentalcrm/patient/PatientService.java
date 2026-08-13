package com.dentalcrm.patient;
import com.dentalcrm.common.NotFoundException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*; import java.util.List; import static com.dentalcrm.patient.PatientDtos.*;
@Service @Transactional
public class PatientService {private final PatientRepository repo;public PatientService(PatientRepository r){repo=r;}
 public PatientResponse create(PatientRequest r){Patient p=new Patient();apply(p,r);return map(repo.save(p));}
 public PatientResponse update(Long id,PatientRequest r){Patient p=get(id);apply(p,r);return map(p);}
 @Transactional(readOnly=true) public List<PatientResponse> all(String search){var result=search==null||search.isBlank()?repo.findAll():repo.findByFullNameContainingIgnoreCaseOrPhoneContaining(search,search);return result.stream().map(this::map).toList();}
 @Transactional(readOnly=true) public PatientResponse find(Long id){return map(get(id));}
 public Patient get(Long id){return repo.findById(id).orElseThrow(()->new NotFoundException("Пациент не найден: "+id));}
 private void apply(Patient p,PatientRequest r){if(r.birthDate()!=null&&!r.birthDate().isBefore(LocalDate.now(ZoneId.of("Asia/Bishkek"))))throw new IllegalArgumentException("Дата рождения должна быть в прошлом.");p.setFullName(r.fullName().trim());p.setPhone(r.phone().trim());p.setBirthDate(r.birthDate());p.setNotes(r.notes());}
 private PatientResponse map(Patient p){return new PatientResponse(p.getId(),p.getFullName(),p.getPhone(),p.getBirthDate(),p.getNotes(),p.getCreatedAt());}
}
