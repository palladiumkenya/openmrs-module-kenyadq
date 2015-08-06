package org.openmrs.module.kenyadq.api.impl;

import au.com.bytecode.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by gitahi on 28/07/15.
 */
@Component
public class CsvCreator {

    public byte[] createCsv(List<Object> data, List<Object> headerRow) {
        StringWriter stringWriter = new StringWriter();
        CSVWriter writer = new CSVWriter(stringWriter);
        try {
            if (headerRow != null) {
                data.add(0, headerRow.toArray());
            }
            for (Object object : data) {
                Object[] values = (Object[]) object;
                String[] row = new String[values.length];
                int i = 0;
                for (Object value : values) {
                    row[i] = value != null ? value.toString() : null;
                    i++;
                }
                writer.writeNext(row);
                Map<Integer, Map<String, Object>> e;
            }
            return stringWriter.toString().getBytes();
        } catch (Exception ex) {
            throw new RuntimeException("Could not download data dictionary.");
        }
    }
}
