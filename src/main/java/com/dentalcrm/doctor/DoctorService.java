package com.dentalcrm.doctor;
import com.dentalcrm.auth.UserSessionRegistry; import com.dentalcrm.common.*; import com.dentalcrm.user.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List; import static com.dentalcrm.doctor.DoctorDtos.*; import static com.dentalcrm.user.UserDtos.*;
@Service @Transactional
public class DoctorService {
 private final DoctorRepository doctors; private final UserRepository users; private final PasswordEncoder encoder; private final UserSessionRegistry sessions;
 public DoctorService(DoctorRepository d,UserRepository u,PasswordEncoder e,UserSessionRegistry sessions){doctors=d;users=u;encoder=e;this.sessions=sessions;}
 public DoctorResponse create(CreateDoctorUserRequest r){String username=r.username().trim();if(users.existsByUsernameIgnoreCase(username))throw new ConflictException("Это имя пользователя уже занято.");User u=new User();u.setUsername(username);u.setPasswordHash(encoder.encode(r.password()));u.setFullName(r.fullName().trim());u.setRole(Role.DOCTOR);u.setActive(true);users.save(u);Doctor d=new Doctor();d.setUser(u);d.setSpecialization(trim(r.specialization()));d.setPhone(trim(r.phone()));return map(doctors.save(d));}
 @Transactional(readOnly=true) public List<DoctorResponse> findAll(){return doctors.findAllByOrderByUserFullName().stream().map(this::map).toList();}
 @Transactional(readOnly=true) public List<ActiveDoctorResponse> findActive(){return doctors.findByUserActiveTrueOrderByUserFullName().stream().map(this::mapActive).toList();}
 @Transactional(readOnly=true) public DoctorResponse find(Long id){return map(get(id));}
 public DoctorResponse update(Long id,UpdateDoctorRequest r){Doctor d=get(id);User u=users.findByIdForUpdate(d.getUser().getId()).orElseThrow();u.setFullName(r.fullName().trim());d.setUser(u);d.setSpecialization(trim(r.specialization()));d.setPhone(trim(r.phone()));return map(d);}
 public DoctorResponse setActive(Long id,boolean active){Doctor d=get(id);User u=users.findByIdForUpdate(d.getUser().getId()).orElseThrow();boolean deactivating=u.isActive()&&!active;u.setActive(active);if(deactivating){u.setAuthVersion(u.getAuthVersion()+1);sessions.revokeAll(u.getUsername());}d.setUser(u);return map(d);}
 public void resetPassword(Long id,String password){Doctor d=get(id);User u=users.findByIdForUpdate(d.getUser().getId()).orElseThrow();u.setPasswordHash(encoder.encode(password));u.setAuthVersion(u.getAuthVersion()+1);sessions.revokeAll(u.getUsername());}
 public Doctor getForAssignment(Long id){Doctor d=get(id);User u=users.findByIdForUpdate(d.getUser().getId()).orElseThrow();d.setUser(u);return d;}
 public Doctor get(Long id){return doctors.findById(id).orElseThrow(()->new NotFoundException("Врач не найден: "+id));}
 private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
 private ActiveDoctorResponse mapActive(Doctor d){return new ActiveDoctorResponse(d.getId(),d.getUser().getFullName(),d.getSpecialization());}
 private DoctorResponse map(Doctor d){var u=d.getUser();return new DoctorResponse(d.getId(),u.getId(),u.getUsername(),u.getFullName(),d.getSpecialization(),d.getPhone(),u.isActive());}
}
