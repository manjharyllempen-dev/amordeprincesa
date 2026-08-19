// Modo de cobro: efectivo + transferencia manual opcional.
async function cashSettings(){
  try{
    const d=await api('/rest/v1/store_settings?select=*&id=eq.1');
    settings=d?.[0]||{};
    return settings;
  }catch{return settings||{};}
}

choosePayment = async function(method){
  const box=document.getElementById('paymsg');
  if(box) box.innerHTML=msg('Creando pedido y validando stock...');
  try{
    const id=await ensureOrder();
    const s=await cashSettings();
    const selectedMethod=method==='manual_transfer'?'manual_transfer':'cash';
    if(selectedMethod==='manual_transfer' && !(s.manual_transfer_enabled && (s.bank_alias||s.bank_cbu))){
      throw new Error('La transferencia manual todavía no está configurada. Elegí pago en efectivo.');
    }
    let ref=null;
    if(selectedMethod==='manual_transfer') ref=prompt('Referencia o comprobante de transferencia (opcional):','')||null;
    await api('/rest/v1/rpc/set_own_order_payment_method',{method:'POST',body:JSON.stringify({p_order_id:id,p_method:selectedMethod,p_reference:ref})});
    go(11);
  }catch(e){
    if(box) box.innerHTML=msg(e.message.includes('insufficient_stock')?'No hay stock suficiente para esta compra.':e.message,'err');
    try{await loadProducts(false)}catch{}
  }
};

saveSettings = async function(){
  try{
    const body={
      store_name:document.getElementById('sname')?.value||'Amor de Princesa',
      description:document.getElementById('sdesc')?.value||'',
      whatsapp:document.getElementById('swa')?.value||'',
      email:document.getElementById('semail')?.value||'',
      instagram:document.getElementById('sinsta')?.value||'',
      shipping_cost_ars:Number(document.getElementById('sship')?.value||0),
      mercado_pago_enabled:false,
      cash_enabled:true,
      manual_transfer_enabled:document.getElementById('smanual')?.checked||false,
      transfer_enabled:document.getElementById('smanual')?.checked||false,
      pay_on_pickup_enabled:true,
      bank_alias:document.getElementById('salias')?.value.trim()||null,
      bank_cbu:document.getElementById('scbu')?.value.trim()||null,
      bank_holder:document.getElementById('sholder')?.value.trim()||null,
      bank_name:document.getElementById('sbank')?.value.trim()||null,
      updated_at:new Date().toISOString()
    };
    await api('/rest/v1/store_settings?id=eq.1',{method:'PATCH',headers:{Prefer:'return=minimal'},body:JSON.stringify(body)});
    settings={...(settings||{}),...body};
    const out=document.getElementById('sout'); if(out) out.innerHTML=msg('Configuración de cobro guardada.');
  }catch(e){const out=document.getElementById('sout'); if(out) out.innerHTML=msg(e.message,'err');}
};

async function markOrderPaid(paid=true){
  const id=typeof lastOrder==='object'?lastOrder?.id:lastOrder;
  if(!id) return alert('No se encontró el pedido.');
  try{
    const ref=paid && lastOrder?.payment_provider==='manual_transfer' ? (prompt('Referencia de transferencia (opcional):',lastOrder?.transfer_reference||'')||null) : null;
    await api('/rest/v1/rpc/admin_mark_order_paid',{method:'POST',body:JSON.stringify({p_order_id:id,p_paid:paid,p_reference:ref})});
    orders=[];
    go(17);
  }catch(e){alert(e.message)}
}

async function renderCashPayment(){
  const h=document.querySelector('.screen h1');
  if(!h||h.textContent.trim()!=='Pago') return;
  const sec=h.closest('.screen'); if(!sec||sec.dataset.cashPatched) return;
  sec.dataset.cashPatched='1';
  const s=await cashSettings();
  const transferReady=!!(s.manual_transfer_enabled && (s.bank_alias||s.bank_cbu));
  const bankInfo=transferReady?`<div class="card"><b>🏦 Transferencia manual</b>${s.bank_alias?`<p><b>Alias:</b> ${esc(s.bank_alias)}</p>`:''}${s.bank_cbu?`<p><b>CBU:</b> ${esc(s.bank_cbu)}</p>`:''}${s.bank_holder?`<p><b>Titular:</b> ${esc(s.bank_holder)}</p>`:''}${s.bank_name?`<p><b>Banco:</b> ${esc(s.bank_name)}</p>`:''}<p class="small">Realizá la transferencia y confirmá el pedido. La tienda verificará el pago manualmente.</p></div><button class="btn soft" onclick="choosePayment('manual_transfer')">CONFIRMAR TRANSFERENCIA MANUAL</button>`:'';
  sec.innerHTML=`<div class="small">Pantalla 11 de 23</div><h1>Pago</h1><p class="small">Elegí cómo vas a pagar</p><div class="card"><b>💵 Pago en efectivo</b><p>Pagás en efectivo al retirar o al recibir, según la modalidad acordada con la tienda.</p></div><button class="btn" onclick="choosePayment('cash')">CONFIRMAR PAGO EN EFECTIVO</button>${bankInfo}<div id="paymsg"></div>`;
}

async function renderCashSettings(){
  const h=document.querySelector('.screen h1');
  if(!h||h.textContent.trim()!=='Configuración') return;
  const sec=h.closest('.screen'); if(!sec||sec.dataset.cashPatched) return;
  sec.dataset.cashPatched='1';
  const s=await cashSettings();
  sec.innerHTML=`<div class="small">Pantalla 23 de 23</div><h1>Configuración</h1><p class="small">Datos de tienda y cobros</p>
  <input id="sname" placeholder="Nombre de la tienda" value="${esc(s.store_name||'Amor de Princesa')}">
  <textarea id="sdesc" placeholder="Descripción">${esc(s.description||'')}</textarea>
  <input id="swa" placeholder="WhatsApp" value="${esc(s.whatsapp||'')}">
  <input id="semail" placeholder="Email" value="${esc(s.email||'')}">
  <input id="sinsta" placeholder="Instagram" value="${esc(s.instagram||'')}">
  <input id="sship" type="number" min="0" placeholder="Costo de envío ARS" value="${Number(s.shipping_cost_ars||0)}">
  <div class="card"><b>💵 Efectivo</b><p>Siempre habilitado.</p></div>
  <label class="card"><input id="smanual" type="checkbox" style="width:auto;min-height:auto" ${s.manual_transfer_enabled?'checked':''}> Habilitar transferencia manual</label>
  <input id="salias" placeholder="Alias" value="${esc(s.bank_alias||'')}">
  <input id="scbu" placeholder="CBU" value="${esc(s.bank_cbu||'')}">
  <input id="sholder" placeholder="Titular de la cuenta" value="${esc(s.bank_holder||'')}">
  <input id="sbank" placeholder="Banco" value="${esc(s.bank_name||'')}">
  <div class="msg ok">Cobros configurados sin pasarela online: efectivo y transferencia manual opcional.</div>
  <div id="sout"></div><button class="btn" onclick="saveSettings()">GUARDAR CONFIGURACIÓN</button>`;
}

function renderAdminPaymentControls(){
  const h=document.querySelector('.screen h1');
  if(!h||h.textContent.trim()!=='Detalle pedido'||profile?.role!=='admin') return;
  const sec=h.closest('.screen'); if(!sec||sec.querySelector('#manual-payment-admin')) return;
  const method=lastOrder?.payment_provider==='manual_transfer'?'Transferencia manual':'Efectivo';
  const paid=lastOrder?.payment_status==='paid';
  const box=document.createElement('div');
  box.id='manual-payment-admin'; box.className='card';
  box.innerHTML=`<b>Cobro: ${method}</b><p>Estado: <span class="status">${paid?'Pagado':'Pendiente'}</span></p>${paid?'<button class="btn soft" onclick="markOrderPaid(false)">MARCAR COMO PENDIENTE</button>':'<button class="btn" onclick="markOrderPaid(true)">MARCAR COMO COBRADO</button>'}`;
  sec.appendChild(box);
}

function removeMercadoPagoText(){
  document.querySelectorAll('.screen *').forEach(el=>{
    if(el.children.length===0 && /mercado\s*pago/i.test(el.textContent||'')) el.textContent=(el.textContent||'').replace(/mercado\s*pago/ig,'Transferencia manual');
  });
}

const cashObserver=new MutationObserver(()=>{
  renderCashPayment();
  renderCashSettings();
  renderAdminPaymentControls();
  removeMercadoPagoText();
});
cashObserver.observe(document.getElementById('main'),{childList:true,subtree:true});
setTimeout(()=>{renderCashPayment();renderCashSettings();renderAdminPaymentControls();removeMercadoPagoText();},50);
