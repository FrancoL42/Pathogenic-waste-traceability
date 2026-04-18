package ar.edu.utn.frc.tup.lciii.services;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public interface EnvioCorreosService {
    void enviarCorreo(String para, String asunto, String cuerpo);

    void enviarCorreoConAdjunto(String para, String asunto, String cuerpo, String nombreAdjunto, byte[] contenidoAdjunto);

    void enviarCorreoConAdjuntoRecurso(String para, String asunto, String cuerpo, String nombreAdjunto, Resource recurso);
}
