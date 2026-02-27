(function(){
  const menuBtns = Array.from(document.querySelectorAll('.menu-btn'));
  const panels = Array.from(document.querySelectorAll('.panel'));
  const nameInput = document.getElementById('nameInput');
  const phoneInput = document.getElementById('phoneInput');
  const emailInput = document.getElementById('emailInput');
  const profileName = document.getElementById('profileName');
  const profileEmail = document.getElementById('profileEmail');
  const profileAvatar = document.getElementById('profileAvatar');
  const loginHistoryBody = document.getElementById('loginHistoryBody');
  const deviceList = document.getElementById('deviceList');

  function activate(menu){
    menuBtns.forEach(b=>b.classList.toggle('active', b.dataset.menu===menu));
    panels.forEach(p=>p.classList.toggle('active', p.dataset.panel===menu));
  }

  menuBtns.forEach(btn=>btn.addEventListener('click',()=>activate(btn.dataset.menu)));

  async function loadMe(){
    const res = await fetch('/api/settings/me');
    if(!res.ok) return;
    const me = await res.json();

    nameInput.value = me.name || '';
    phoneInput.value = me.phone || '';
    emailInput.value = me.email || me.username || '';

    profileName.textContent = me.name || me.username || '사용자';
    profileEmail.textContent = me.email || me.username || '-';
    if(profileName.textContent) profileAvatar.textContent = profileName.textContent.substring(0,1);

    loginHistoryBody.innerHTML = '';
    (me.loginHistory || []).forEach(v=>{
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${v.loginTime||'-'}</td><td>${v.ipAddress||'-'}</td><td>${v.userAgent||'-'}</td>`;
      loginHistoryBody.appendChild(tr);
    });
    if((me.loginHistory||[]).length===0){
      const tr=document.createElement('tr'); tr.innerHTML='<td colspan="3">로그인 기록이 없습니다.</td>'; loginHistoryBody.appendChild(tr);
    }

    deviceList.innerHTML='';
    (me.devices || []).forEach(d=>{
      const li = document.createElement('li');
      li.textContent = `${d.userAgent || '-'} / ${d.ipAddress || '-'} / 최근 ${d.lastLoginTime || '-'}`;
      deviceList.appendChild(li);
    });
  }

  document.getElementById('saveProfileBtn')?.addEventListener('click', async ()=>{
    const res = await fetch('/api/settings/profile', {
      method: 'PUT',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({name:nameInput.value, phone:phoneInput.value})
    });
    if(!res.ok){ alert('저장 실패'); return; }
    await loadMe();
    alert('저장되었습니다.');
  });

  document.getElementById('changePwBtn')?.addEventListener('click', async ()=>{
    const currentPassword = document.getElementById('currentPw').value;
    const newPassword = document.getElementById('newPw').value;
    const res = await fetch('/api/settings/password', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify({currentPassword, newPassword})
    });
    const json = await res.json().catch(()=>({message:'변경 실패'}));
    alert(json.message || (res.ok?'변경 완료':'변경 실패'));
  });

  document.getElementById('withdrawBtn')?.addEventListener('click', async ()=>{
    if(!confirm('정말 회원 탈퇴하시겠습니까?')) return;
    const res = await fetch('/api/settings/account', {method:'DELETE'});
    const json = await res.json().catch(()=>({}));
    if(res.ok){ alert(json.message || '탈퇴되었습니다.'); location.href='/login?withdrawn'; }
    else alert(json.message || '탈퇴 실패');
  });

  loadMe();
})();
