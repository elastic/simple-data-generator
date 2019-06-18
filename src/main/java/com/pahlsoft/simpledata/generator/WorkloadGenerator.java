package com.pahlsoft.simpledata.generator;

import co.elastic.apm.api.CaptureSpan;
import com.github.javafaker.Faker;

import java.util.*;

import com.pahlsoft.simpledata.model.Workload;

public class WorkloadGenerator {

    WorkloadGenerator() {
        throw new IllegalStateException("Utility class");
    }

    @CaptureSpan
    public static Map buildDocument(Workload workload) {
        Faker faker = new Faker(new Locale("en-US"));
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("@timestamp", new Date());

        // Go through each field in the workload
        Iterator iterator = workload.getFields().iterator();
        Map<String,String> field;

        while (iterator.hasNext()) {
            field = (Map<String,String>) iterator.next();
            String state = "";

            switch(field.get("type")){
                case "empty":
                    jsonMap.put(field.get("name"),"");
                    break;
                case "int":
                    if (field.get("range") != null) {
                        String[] range = field.get("range").split(",");
                        jsonMap.put(field.get("name"),faker.number().numberBetween(Integer.valueOf(range[0]),Integer.valueOf(range[1])));
                    } else {
                        jsonMap.put(field.get("name"),faker.number().numberBetween(0,65336));
                    }
                    break;
                case "float":
                    if (field.get("range") != null) {
                        String[] range = field.get("range").split(",");
                        jsonMap.put(field.get("name"),faker.number().randomDouble(2,Integer.valueOf(range[0]),Integer.valueOf(range[1])));
                    } else {
                        jsonMap.put(field.get("name"),faker.number().randomDouble(2,0,65336));
                    }
                    break;
                case "boolean":
                    jsonMap.put(field.get("name"),faker.bool().bool());
                    break;
                case "full_name":
                    jsonMap.put(field.get("name"),faker.name().fullName());
                     break;
                case "first_name":
                    jsonMap.put(field.get("name"),faker.name().firstName());
                    break;
                case "last_name":
                    jsonMap.put(field.get("name"),faker.name().lastName());
                    break;
                case "full_address":
                    jsonMap.put(field.get("name"),faker.address().fullAddress());
                    break;
                case "street_address":
                    jsonMap.put(field.get("name"),faker.address().streetAddress());
                    break;
                case "city":
                    jsonMap.put(field.get("name"),faker.address().cityName());
                    break;
                case "country":
                    jsonMap.put(field.get("name"),faker.address().country());
                    break;
                case "country_code":
                    jsonMap.put(field.get("name"),faker.address().countryCode());
                    break;
                case "state":
                    state = faker.address().stateAbbr();
                    jsonMap.put("state",state);
                    break;
                case "zipcode":
                    jsonMap.put("name",faker.address().zipCodeByState(state));
                    break;
                case "geo_point":
                    jsonMap.put("geo_point","{\"lon:\" " + faker.address().longitude() + " }, {\"lat:\" " + faker.address().longitude() + "}");
                    break;
                case "phone_number":
                    jsonMap.put("phone_number",faker.phoneNumber().cellPhone());
                    break;
                case "credit_card_number":
                    jsonMap.put(field.get("name"),faker.business().creditCardNumber());
                    break;
                case "ssn":
                    jsonMap.put(field.get("name"), faker.idNumber().ssnValid());
                    break;
                case "product_name":
                    jsonMap.put(field.get("name"),faker.commerce().productName());
                    break;
                case "group":
                    jsonMap.put(field.get("name"),faker.commerce().department());
                    break;
                case "uuid":
                    jsonMap.put(field.get("name"),UUID.randomUUID().toString());
                    break;
                case "path":
                    jsonMap.put(field.get("name"),faker.file().fileName());
                    break;
                case "hostname":
                    jsonMap.put(field.get("name"),faker.ancient().god());
                    break;
                case "appname":
                    jsonMap.put(field.get("name"),faker.app().name());
                case "url":
                    jsonMap.put(field.get("name"),faker.internet().url());
                    break;
                case "random_string_from_list":
                    if (field.get("custom_list") != null) {
                        String[] range = field.get("custom_list").split(",");

                        jsonMap.put(field.get("name"),getRandomString(range));
                    } else {
                        System.out.println("Improper Mapping Definition");
                    }
                    break;
                case "random_integer_from_list":
                    if (field.get("custom_list") != null) {
                        String[] range = field.get("custom_list").split(",");

                        jsonMap.put(field.get("name"),Integer.valueOf(getRandomValues(range)));
                    } else {
                        System.out.println("Improper Mapping Definition");
                    }
                    break;
                case "random_float_from_list":
                    if (field.get("custom_list") != null) {
                        String[] range = field.get("custom_list").split(",");

                        jsonMap.put(field.get("name"),Float.valueOf(getRandomValues(range)));
                    } else {
                        System.out.println("Improper Mapping Definition");
                    }
                    break;
                case "random_long_from_list":
                    if (field.get("custom_list") != null) {
                        String[] range = field.get("custom_list").split(",");

                        jsonMap.put(field.get("name"),Long.valueOf(getRandomValues(range)));
                    } else {
                        System.out.println("Improper Mapping Definition");
                    }
                    break;
                case "ipv4":
                    jsonMap.put(field.get("name"),faker.internet().ipV4Address());
                    break;
                case "mac_address":
                    jsonMap.put(field.get("name"),faker.internet().macAddress());
                    break;
                case "email":
                    jsonMap.put(field.get("name"),faker.internet().emailAddress());
                    break;
                case "domain":
                    jsonMap.put(field.get("name"),faker.internet().domainName());
                    break;
                case "hash":
                    jsonMap.put(field.get("name"),faker.crypto().sha512());
                    break;
                case "random_cn_fact":
                    jsonMap.put(field.get("name"),faker.chuckNorris().fact());
                    break;
                case "random_got_character":
                    jsonMap.put(field.get("name"),faker.gameOfThrones().character());
                    break;
                case "random_occupation":
                    jsonMap.put(field.get("name"),faker.job().title());
                    break;
                case "iban":
                    jsonMap.put(field.get("name"),faker.finance().iban());
                    break;
                case "team_name":
                    jsonMap.put(field.get("name"),faker.team().name());
                    break;
                case "constant_string" :
                    jsonMap.put(field.get("name"),field.get("value"));
                    break;
                case "date":
                    jsonMap.put(field.get("name"),new Date());
                    break;
                case "timezone":
                    jsonMap.put(field.get("name"),faker.address().timeZone());
                    break;
                default:
                    jsonMap.put("unmapped type","unmapped type");
                    System.out.println("Warning: unmapped Type: " + field.get("name"));
                    break;
            }

        }

        return jsonMap;
    }

    private static String getRandomString(String[] listOfThings) {
        return listOfThings[getRandomInteger(0,listOfThings.length - 1)];
    }

    private static String getRandomValues(String[] listOfThings) {
        return listOfThings[getRandomInteger(0,listOfThings.length - 1)];
    }

    private static synchronized int getRandomInteger(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;

    }


}
