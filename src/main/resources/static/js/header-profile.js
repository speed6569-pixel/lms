document.addEventListener('DOMContentLoaded', () => {
  const wrap = document.getElementById('profileWrap');
  const btn = document.getElementById('profileBtn');
  const dropdown = document.getElementById('profileDropdown');
  const avatar = document.getElementById('avatarText');
  if (!wrap || !btn || !dropdown) return;

  const strong = wrap.querySelector('.profile-text strong');
  const name = strong ? strong.textContent.trim() : '';
  if (avatar && name) avatar.textContent = name.substring(0, 1).toUpperCase();

  btn.addEventListener('click', (e) => {
    e.stopPropagation();
    dropdown.classList.toggle('open');
  });

  document.addEventListener('click', (e) => {
    if (!wrap.contains(e.target)) dropdown.classList.remove('open');
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') dropdown.classList.remove('open');
  });
});
