let toc = document.querySelector("#TableOfContents")

document.querySelector("#toc-button").addEventListener('click', (e) => {
  e.preventDefault()
  toc.classList.toggle("show")
})
