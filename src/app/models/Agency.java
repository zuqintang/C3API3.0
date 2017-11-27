package app.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("edc_agency")
@IdName("AGENCY_ID")
public class Agency extends Model {
}
