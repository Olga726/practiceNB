package requesters.sceleton.interfaces;

import models.BaseModel;

public interface CrudEndpointInterface {
    Object post(BaseModel model);
    Object get(long id);
    Object getAll();
    Object update(BaseModel model);
    Object delete(long id);
}
