package app.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("units")
@IdName("FEATURE_IDENTITY")
public class Unit extends Model {
}
