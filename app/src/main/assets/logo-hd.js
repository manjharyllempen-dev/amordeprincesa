(()=>{
  const useValidLogo=()=>{
    document.querySelectorAll('img').forEach(img=>{
      const src=(img.getAttribute('src')||'').toLowerCase();
      if(src.includes('logo.webp') || src.endsWith('logo.png')){
        if(img.getAttribute('src')!=='logo.png') img.setAttribute('src','logo.png');
        img.style.imageRendering='auto';
        img.style.opacity='1';
      }
    });
  };
  useValidLogo();
  new MutationObserver(useValidLogo).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['src']});
})();
