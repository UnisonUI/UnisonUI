let toc = document.querySelector(".left-menu")

document.querySelector("#toc-button").addEventListener('click', (e) => {
  e.preventDefault()
  toc.classList.toggle("show")
})
