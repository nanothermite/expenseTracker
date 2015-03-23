package models;

import com.avaje.ebean.annotation.Sql;

import javax.persistence.Entity;

@Entity
@Sql
public class Years {
    public Integer year;

    public Integer getYear() {
        return year;
    }
    public void setYear(Integer year) {
        this.year = year;
    }
}