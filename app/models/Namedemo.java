package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Namedemo extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public String name;

    public String title;
    public String email;

    public static Model.Finder<Long, Namedemo> find = new Model.Finder(Long.class, Namedemo.class);

    public static List<Namedemo> findAll() {
        return find.all();
    }

    public String toString() {
        return name;
    }

    public static List<String> getColOrder() {
        List<String> colOrder = new ArrayList(3);
        colOrder.add("name");
        colOrder.add("title");
        colOrder.add("email");
        return colOrder;
    }
}