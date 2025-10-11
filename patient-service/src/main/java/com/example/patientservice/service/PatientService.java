package com.example.patientservice.service;

import com.example.patientservice.dto.PatientReponseDTO;
import com.example.patientservice.dto.PatientRequestDTO;
import com.example.patientservice.exception.EmailAlreadyExistsException;
import com.example.patientservice.mapper.PatientMapper;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientReponseDTO> getPatients () {
        List<Patient> patients = patientRepository.findAll();
        List<PatientReponseDTO> patientResponseDTOs = patients.stream().map(PatientMapper::toDTO).toList();
        return patientResponseDTOs;
    }

    public PatientReponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with this email already exists" + patientRequestDTO.getEmail());
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        return PatientMapper.toDTO(newPatient);
    }

}
