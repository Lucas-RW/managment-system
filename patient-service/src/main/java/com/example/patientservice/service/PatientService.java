package com.example.patientservice.service;

import com.example.patientservice.dto.PatientReponseDTO;
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

}
