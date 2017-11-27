package app.controllers;


import app.models.Medicine;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;

import java.util.List;
import java.util.Map;

public class HospitalSvrController extends BasicController {
    public void index() {
        respond("This is HospitalSvr");
    }

    @POST
    public void getMedicines() {
        String kw = null == param("kw") ? "" :param("kw");
        int p = null == param("p")|| ""== param("p") ? 1 : Integer.parseInt(param("p"));
        int pagesize = null == param("pagesize") || "" == param("pagesize") ? 10 : Integer.parseInt(param("pagesize"));
        LazyList<Model> medicineList = Medicine.where("NAME like ?","%"+kw+"%")
                .offset((p-1)*pagesize)
                .limit(pagesize);
        List<Map<String,Object>> data =medicineList.toMaps();
        double countPage = data.size();
        output(0,data,countPage);
    }
}
