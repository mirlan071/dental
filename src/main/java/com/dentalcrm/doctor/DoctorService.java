package com.dentalcrm.doctor;
import com.dentalcrm.common.*; import com.dentalcrm.user.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List; import static com.dentalcrm.doctor.DoctorDtos.*; import static com.dentalcrm.user.UserDtos.*;
@Service @Transactional
public class DoctorService {
 private final DoctorRepository doctors; private final UserRepository users; private final PasswordEncoder encoder;
 public DoctorService(DoctorRepository d,UserRepository u,PasswordEncoder e){doctors=d;users=u;encoder=e;}
 public DoctorResponse create(CreateDoctorUserRequest r){String username=r.username().trim();if(users.existsByUsernameIgnoreCase(username))throw new ConflictException("Username already exists");User u=new User();u.setUsername(username);u.setPasswordHash(encoder.encode(r.password()));u.setFullName(r.fullName().trim());u.setRole(Role.DOCTOR);u.setActive(true);users.save(u);Doctor d=new Doctor();d.setUser(u);d.setSpecialization(trim(r.specialization()));d.setPhone(trim(r.phone()));return map(doctors.save(d));}
 @Transactional(readOnly=true) public List<DoctorResponse> findAll(boolean activeOnly){return (activeOnly?doctors.findByUserActiveTrueOrderByUserFullName():doctors.findAll()).stream().map(this::map).toList();}
 @Transactional(readOnly=true) public DoctorResponse find(Long id){return map(get(id));}
 public DoctorResponse update(Long id,UpdateDoctorRequest r){Doctor d=get(id);d.getUser().setFullName(r.fullName().trim());d.setSpecialization(trim(r.specialization()));d.setPhone(trim(r.phone()));return map(d);}
 public DoctorResponse setActive(Long id,boolean active){Doctor d=get(id);d.getUser().setActive(active);return map(d);}
 public void resetPassword(Long id,String password){get(id).getUser().setPasswordHash(encoder.encode(password));}
 public Doctor get(Long id){return doctors.findById(id).orElseThrow(()->new NotFoundException("Doctor not found: "+id));}
 private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
 private DoctorResponse map(Doctor d){var u=d.getUser();return new DoctorResponse(d.getId(),u.getId(),u.getUsername(),u.getFullName(),d.getSpecialization(),d.getPhone(),u.isActive());}
}
