function fixSingleHomeLogo(){
  const sec=document.querySelector('#main .screen');
  if(!sec) return;
  const isHome=(typeof i!=='undefined' && i===0) || sec.querySelector('h1')?.textContent.trim()==='Inicio' || sec.querySelector('h1')?.textContent.trim()==='Bienvenida';
  if(!isHome) return;

  // Quitar cualquier logo grande duplicado insertado por scripts anteriores.
  sec.querySelectorAll('.screen-brand-logo').forEach(el=>el.remove());

  // Dejar una sola imagen principal grande.
  const heroImg=sec.querySelector('.hero img');
  if(heroImg){
    heroImg.src='logo_hd.jpg';
    heroImg.alt='Amor de Princesa';
    heroImg.classList.add('home-single-logo');
  }

  const head=document.querySelector('.head');
  if(head) head.style.display='flex';
}

const singleLogoObserver=new MutationObserver(()=>fixSingleHomeLogo());
singleLogoObserver.observe(document.getElementById('main'),{childList:true,subtree:true});
setTimeout(fixSingleHomeLogo,60);
setTimeout(fixSingleHomeLogo,250);
