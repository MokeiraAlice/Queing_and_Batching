package com.example.Processor;


import com.example.entity.Person;
import org.springframework.batch.item.ItemProcessor;

public class PersonItemProcessor implements ItemProcessor<Person, Person> {
    @Override
    public Person process(Person person) throws Exception {
        // You can add any transformation logic here
        // For example, converting names to uppercase

        Person transformedPerson = new Person();
        transformedPerson.setId(person.getId());
        transformedPerson.setFirstName(person.getFirstName().toUpperCase());
        transformedPerson.setLastName(person.getLastName().toUpperCase());
        transformedPerson.setEmail(person.getEmail());
        transformedPerson.setBirthDate(person.getBirthDate());

        return transformedPerson;
    }
}