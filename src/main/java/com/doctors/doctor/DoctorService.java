package com.doctors.doctor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.doctors.util.EngagedAppointmentException;
import com.doctors.util.InvalidDateException;
import com.doctors.util.NoSuchDoctorException;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final JpaDoctorRepository doctorRepository;

    public Page<Doctor> getDoctors(Optional<List<String>> specializations,
                                   Optional<String> name,
                                   Pageable pageable) {
        if (specializations.isPresent() && name.isPresent()) {
            return doctorRepository.findBySpecializationsInAndName(specializations.get(), name.get(), pageable);
        }
        if (specializations.isPresent()) {
            return doctorRepository.findBySpecializationsIgnoreCaseIn(specializations
                    .get(), pageable);
        }
        if (name.isPresent()) {
            return doctorRepository.findByNameIgnoreCase(name.get(), pageable);
        }
        return doctorRepository.findAll(pageable);
    }

    public Optional<Doctor> getById(Integer id) {

        return doctorRepository.findById(id);
    }

    public Doctor save(Doctor doctor) {
        return doctorRepository.save(doctor);

    }

    public Optional<Doctor> delete(Integer id) {
        Optional<Doctor> maybeDoctor = doctorRepository.findById(id);

        maybeDoctor.ifPresent(doctor -> doctorRepository.delete(doctor.getId()));

        return maybeDoctor;
    }


    public Boolean doctorExists(Integer id) {
        return doctorRepository.findAll().stream().anyMatch(doc -> doc.getId().equals(id));
    }

    public Map<LocalTime, Integer> getDoctorsSchedule(Integer id, LocalDate date) {
        if (!doctorRepository.findById(id).isPresent()) {
            throw new NoSuchDoctorException();
        }

        return doctorRepository.findById(id)
                .get()
                .getAppointments().stream()
                .filter(appointment -> appointment.getComingDate().equals(date))
                .collect(Collectors.toMap(Appointment::getTime, Appointment::getPetId));

    }

    @Transactional
    public Doctor createAnAppointment(Integer doctorId, LocalDate date, LocalTime time, Integer petId) {
        if (!doctorRepository.findById(doctorId).isPresent()) {
            throw new NoSuchDoctorException();
        }
        Doctor doctor = doctorRepository.findById(doctorId).get();
        Appointment appointment = new Appointment(date, time, petId);
        if (appointment.getComingDate().isBefore(LocalDate.now()) ||
                appointment.getTime().isBefore(LocalTime.of(8, 0)) ||
                appointment.getTime().isAfter(LocalTime.of(16, 0)) ||
                appointment.getTime().getMinute() != 0) {
            throw new InvalidDateException();
        }
        if (doctor.getAppointments().
                stream()
                .anyMatch(appointment1 -> appointment1.getComingDate().equals(date) &&
                        appointment1.getTime().equals(time))) {
            throw new EngagedAppointmentException();
        }
        doctor.getAppointments().add(appointment);
        return doctorRepository.save(doctor);
    }

}
