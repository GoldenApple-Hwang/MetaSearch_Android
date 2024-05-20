package com.example.metasearch.model.response;

import java.util.List;

public class PersonFrequencyResponse {
    private List<Frequency> frequencies;

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public void setFrequencies(List<Frequency> frequencies) {
        this.frequencies = frequencies;
    }

    public static class Frequency {
        private String personName;
        private int frequency;

        public String getPersonName() {
            return personName;
        }

        public void setPersonName(String personName) {
            this.personName = personName;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }
    }
}
