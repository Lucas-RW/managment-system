package com.example.patientservice.mapper;

import com.example.patientservice.dto.PatientReponseDTO;
import com.example.patientservice.model.Patient;

public class PatientMapper {

    public static PatientReponseDTO toDTO(Patient patient) {

        PatientReponseDTO patientDTO = new PatientReponseDTO();
        patientDTO.setId(patient.getId().toString());
        patientDTO.setName(patient.getName());
        patientDTO.setAddress(patient.getAddress());
        patientDTO.setEmail(patient.getEmail());
        patientDTO.setDateOfBirth(patient.getDateOfBirth().toString());

        return patientDTO;

    }

}
