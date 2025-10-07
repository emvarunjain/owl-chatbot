(function(){
  function createWidget(baseUrl){
    const box = document.createElement('div');
    box.style.cssText='position:fixed;bottom:20px;right:20px;width:320px;height:420px;background:#fff;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.1);display:flex;flex-direction:column;overflow:hidden;font-family:sans-serif;z-index:99999';
    box.innerHTML=`<div style="padding:.5rem;background:#111;color:#fff">OWL Chat</div>
    <div id="owl-log" style="flex:1;padding:.5rem;overflow:auto"></div>
    <div style="display:flex;padding:.5rem;gap:.25rem"><input id="owl-input" style="flex:1;padding:.5rem" placeholder="Ask..."/><button id="owl-send">Send</button></div>`;
    document.body.appendChild(box);
    const log = box.querySelector('#owl-log');
    const input = box.querySelector('#owl-input');
    box.querySelector('#owl-send').onclick = async ()=>{
      const q = input.value.trim(); if(!q) return;
      input.value='';
      append('You', q);
      const tenantId = window.OWL_TENANT_ID || 'demo';
      const res = await fetch((baseUrl||'')+'/api/v1/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({tenantId,question:q,allowWeb:false})});
      const data = await res.json();
      append('Owl', data.answer||JSON.stringify(data));
    };
    function append(who,text){
      const p=document.createElement('div'); p.style.margin='6px 0'; p.innerHTML=`<strong>${who}:</strong> `+escapeHtml(text);
      log.appendChild(p); log.scrollTop=log.scrollHeight;
    }
    function escapeHtml(s){return s.replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]));}
  }
  window.OwlWidget = { mount: createWidget };
})();
