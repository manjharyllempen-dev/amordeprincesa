const ACCOUNT_REGISTER_FN='register-account';

async function publicEdge(name, body){
  const r=await fetch(`${SB}/functions/v1/${name}`,{
    method:'POST',
    headers:{apikey:KEY,'Content-Type':'application/json'},
    body:JSON.stringify(body)
  });
  const d=await r.json().catch(()=>({}));
  if(!r.ok) throw new Error(d.error||'No se pudo completar la operación.');
  return d;
}

window.register=async function(){
  const name=document.getElementById('rname')?.value.trim()||'';
  const phone=document.getElementById('rphone')?.value.trim()||'';
  const email=document.getElementById('remail')?.value.trim().toLowerCase()||'';
  const password=document.getElementById('rpass')?.value||'';
  const box=document.getElementById('rmsg');
  if(!name||!email||password.length<8){
    if(box) box.innerHTML=msg('Completá nombre y correo. La contraseña debe tener al menos 8 caracteres.','err');
    return;
  }
  if(box) box.innerHTML=msg('Creando cuenta...');
  try{
    await publicEdge(ACCOUNT_REGISTER_FN,{email,password,full_name:name,phone,account_type:'customer'});
    const d=await api('/auth/v1/token?grant_type=password',{method:'POST',body:JSON.stringify({email,password})});
    session=d; localStorage.setItem('ap_session',JSON.stringify(d));
    await loadProfile();
    if(box) box.innerHTML=msg('Cuenta creada y activada.');
    setTimeout(()=>go(3),400);
  }catch(e){ if(box) box.innerHTML=msg(e.message,'err'); }
};

window.registerAdmin=async function(){
  const name=document.getElementById('arname')?.value.trim()||'Administradora';
  const phone=document.getElementById('arphone')?.value.trim()||'';
  const email=document.getElementById('aremail')?.value.trim().toLowerCase()||'';
  const password=document.getElementById('arpass')?.value||'';
  const confirm=document.getElementById('arpass2')?.value||'';
  const box=document.getElementById('adminregmsg');
  if(!email||password.length<8){ if(box) box.innerHTML=msg('Ingresá un correo y una contraseña de al menos 8 caracteres.','err'); return; }
  if(password!==confirm){ if(box) box.innerHTML=msg('Las contraseñas no coinciden.','err'); return; }
  if(box) box.innerHTML=msg('Creando cuenta administradora...');
  try{
    await publicEdge(ACCOUNT_REGISTER_FN,{email,password,full_name:name,phone,account_type:'admin'});
    document.getElementById('aemail').value=email;
    document.getElementById('apass').value=password;
    if(box) box.innerHTML=msg('Cuenta administradora creada. Ya podés ingresar.');
  }catch(e){ if(box) box.innerHTML=msg(e.message,'err'); }
};

const oldLogin=window.login;
window.login=async function(admin=false){
  const emailEl=document.getElementById(admin?'aemail':'lemail');
  const passEl=document.getElementById(admin?'apass':'lpass');
  const box=document.getElementById(admin?'amsg':'lmsg');
  if(!emailEl||!passEl) return;
  const email=emailEl.value.trim().toLowerCase(), password=passEl.value;
  if(box) box.innerHTML=msg('Ingresando...');
  try{
    const d=await api('/auth/v1/token?grant_type=password',{method:'POST',body:JSON.stringify({email,password})});
    session=d; localStorage.setItem('ap_session',JSON.stringify(d));
    await loadProfile();
    if(admin && profile?.role!=='admin'){
      session=null; profile=null; localStorage.removeItem('ap_session');
      if(box) box.innerHTML=msg('Esta cuenta no tiene permisos de administrador.','err');
      return;
    }
    if(admin){
      localStorage.setItem('ap_admin_session',JSON.stringify(d));
      localStorage.setItem('ap_biometric_enabled','1');
    }
    go(admin?13:3);
  }catch(e){ if(box) box.innerHTML=msg(e.message,'err'); }
};

window.startBiometric=function(){
  const box=document.getElementById('amsg');
  if(localStorage.getItem('ap_biometric_enabled')!=='1' || !localStorage.getItem('ap_admin_session')){
    if(box) box.innerHTML=msg('Primero iniciá sesión una vez con correo y contraseña para habilitar la huella.','err');
    return;
  }
  if(window.AndroidBiometric?.authenticate){
    if(box) box.innerHTML=msg('Usá tu huella para ingresar...');
    window.AndroidBiometric.authenticate();
  }else if(box){ box.innerHTML=msg('La huella no está disponible en este dispositivo.','err'); }
};

window.onBiometricSuccess=async function(){
  const box=document.getElementById('amsg');
  try{
    const saved=JSON.parse(localStorage.getItem('ap_admin_session')||'null');
    if(!saved?.access_token) throw new Error('Volvé a ingresar con contraseña para renovar la sesión.');
    session=saved; localStorage.setItem('ap_session',JSON.stringify(saved));
    await loadProfile();
    if(profile?.role!=='admin') throw new Error('La sesión guardada ya no tiene permisos de administrador.');
    go(13);
  }catch(e){ if(box) box.innerHTML=msg(e.message,'err'); }
};
window.onBiometricError=function(text){ const box=document.getElementById('amsg'); if(box) box.innerHTML=msg(text||'No se pudo validar la huella.','err'); };

function applyBigBranding(){
  const splash=document.getElementById('splash');
  if(splash){
    const img=splash.querySelector('img');
    if(img){ img.src='logo.webp'; img.classList.add('splash-logo-hd'); }
  }
  document.querySelectorAll('.head img').forEach(img=>{img.src='logo.webp';img.classList.add('head-logo-hd')});
  document.querySelectorAll('.screen').forEach(sec=>{
    if(!sec.querySelector(':scope > .screen-brand-logo')){
      const img=document.createElement('img');
      img.src='logo.webp'; img.alt='Amor de Princesa'; img.className='screen-brand-logo';
      sec.insertBefore(img,sec.firstChild);
    }
  });
  document.querySelectorAll('img[src="logo.png"]').forEach(img=>img.src='logo.webp');
}

function patchAdminAccess(){
  const h=[...document.querySelectorAll('.screen h1')].find(x=>x.textContent.trim()==='Acceso administrador');
  if(!h) return;
  const sec=h.closest('.screen');
  if(!sec||sec.dataset.adminAccessFixed==='1') return;
  sec.dataset.adminAccessFixed='1';
  sec.innerHTML=`<img src="logo.webp" class="screen-brand-logo" alt="Amor de Princesa">
  <div class="small">Pantalla 13 de 23</div><h1>Acceso administrador</h1>
  <p class="small">Ingresá con tu cuenta administradora o creala por primera vez.</p>
  <div class="card admin"><b>Ingresar</b>
    <input id="aemail" type="email" placeholder="Correo administrador">
    <input id="apass" type="password" placeholder="Contraseña">
    <div id="amsg"></div>
    <button class="btn" onclick="login(true)">INGRESAR COMO ADMINISTRADOR</button>
    <button class="btn gold" onclick="startBiometric()">👆 INGRESAR CON HUELLA DIGITAL</button>
    <p class="small">La huella se habilita después del primer ingreso correcto con contraseña.</p>
  </div>
  <details class="card"><summary><b>Crear cuenta administradora</b></summary>
    <input id="arname" placeholder="Nombre y apellido">
    <input id="arphone" placeholder="Teléfono +54">
    <input id="aremail" type="email" placeholder="Correo administrador">
    <input id="arpass" type="password" placeholder="Contraseña (mínimo 8 caracteres)">
    <input id="arpass2" type="password" placeholder="Repetir contraseña">
    <div id="adminregmsg"></div>
    <button class="btn soft" onclick="registerAdmin()">CREAR CUENTA ADMINISTRADORA</button>
  </details>`;
}

const brandObserver=new MutationObserver(()=>{applyBigBranding();patchAdminAccess()});
brandObserver.observe(document.getElementById('main'),{childList:true,subtree:true});
setTimeout(()=>{applyBigBranding();patchAdminAccess()},50);
