package com.dentalcrm.doctor;
import com.dentalcrm.common.*; import com.dentalcrm.user.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List; import static com.dentalcrm.doctor.DoctorDtos.*; import static com.dentalcrm.user.UserDtos.*;
@Service @Transactional
public class DoctorService {
 private final DoctorRepository doctors; private final UserRepository users; private final PasswordEncoder encoder;
 public DoctorService(DoctorRepository d,UserRepository u,PasswordEncoder e){doctors=d;users=u;encoder=e;}
 public DoctorResponse create(CreateDoctorUserRequest r){if(users.existsByUsernameIgnoreCase(r.username()))throw new ConflictException("Username already exists");User u=new User();u.setUsername(r.username().trim());u.setPasswordHash(encoder.encode(r.password()));u.setFullName(r.fullName().trim());u.setRole(Role.DOCTOR);u.setActive(true);users.save(u);Doctor d=new Doctor();d.setUser(u);d.setSpecialization(r.specialization());d.setPhone(r.phone());return map(doctors.save(d));}
 @Transactional(readOnly=true) public List<DoctorResponse> findAll(){return doctors.findAll().stream().map(this::map).toList();}
 @Transactional(readOnly=true) public DoctorResponse find(Long id){return map(get(id));}
 public DoctorResponse update(Long id,UpdateDoctorRequest r){Doctor d=get(id);d.setSpecialization(r.specialization());d.setPhone(r.phone());if(r.active()!=null)d.getUser().setActive(r.active());return map(d);}
 public Doctor get(Long id){return doctors.findById(id).orElseThrow(()->new NotFoundException("Doctor not found: "+id));}
 private DoctorResponse map(Doctor d){var u=d.getUser();return new DoctorResponse(d.getId(),u.getId(),u.getUsername(),u.getFullName(),d.getSpecialization(),d.getPhone(),u.isActive());}
}
