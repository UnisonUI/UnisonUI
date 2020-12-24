let menu = document.querySelector("#menu-mobile")
document.querySelector("#menu-mobile-button").addEventListener('click', (e) => {
  e.preventDefault()
  menu.classList.toggle("hidden")
})
