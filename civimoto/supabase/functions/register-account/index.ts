import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};
const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:cors});

Deno.serve(async (req:Request)=>{
  if(req.method==="OPTIONS") return new Response("ok",{headers:cors});
  if(req.method!=="POST") return json({message:"Método no permitido"},405);
  try{
    const body=await req.json().catch(()=>({}));
    const email=String(body.email||"").trim().toLowerCase();
    const password=String(body.password||"");
    const fullName=String(body.full_name||"").trim();
    const requestedRole=body.role==="chofer"?"chofer":body.role==="admin"?"admin":"usuario";
    if(!/^\S+@\S+\.\S+$/.test(email)) return json({message:"Ingresa un correo válido"},400);
    if(password.length<6) return json({message:"La contraseña debe tener al menos 6 caracteres"},400);
    if(!fullName) return json({message:"Ingresa tu nombre completo"},400);

    const url=Deno.env.get("SUPABASE_URL")||"";
    const service=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")||Deno.env.get("SUPABASE_SECRET_KEY")||"";
    if(!url||!service) return json({message:"Servidor CiviMoto no configurado"},500);
    const admin=createClient(url,service,{auth:{persistSession:false,autoRefreshToken:false}});

    if(requestedRole==="admin"){
      const {count,error:countError}=await admin.from("profiles").select("id",{count:"exact",head:true}).eq("role","admin");
      if(countError) throw countError;
      if((count||0)>0){
        const authHeader=req.headers.get("Authorization")||"";
        const token=authHeader.startsWith("Bearer ")?authHeader.slice(7):"";
        if(!token) return json({message:"Solo un administrador puede crear otro administrador"},403);
        const {data:userData}=await admin.auth.getUser(token);
        const callerId=userData.user?.id;
        if(!callerId) return json({message:"Sesión administrativa inválida"},403);
        const {data:p}=await admin.from("profiles").select("role").eq("id",callerId).maybeSingle();
        if(p?.role!=="admin") return json({message:"Sin permiso"},403);
      }
    }

    const triggerRole=requestedRole==="admin"?"usuario":requestedRole;
    const {data,error}=await admin.auth.admin.createUser({
      email,password,email_confirm:true,
      user_metadata:{full_name:fullName,role:triggerRole}
    });
    if(error){
      const msg=String(error.message||"");
      if(/already|registered|exists/i.test(msg)) return json({message:"Este correo ya está registrado en CiviMoto"},409);
      return json({message:msg||"No se pudo crear la cuenta"},400);
    }
    if(requestedRole==="admin"&&data.user?.id){
      const {error:roleError}=await admin.from("profiles").update({role:"admin",updated_at:new Date().toISOString()}).eq("id",data.user.id);
      if(roleError){await admin.auth.admin.deleteUser(data.user.id);throw roleError;}
    }
    return json({ok:true,user_id:data.user?.id,email,role:requestedRole},201);
  }catch(e){console.error(e);return json({message:"No se pudo crear la cuenta CiviMoto"},500);}
});
