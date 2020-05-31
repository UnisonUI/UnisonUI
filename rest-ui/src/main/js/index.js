import React from 'react'
import ReactDOM from 'react-dom'
import App from './components/ui'

require('./css/main.css')
require('./css/burger-menu.css')
require('./css/swagger-ui/main.scss')

const wrapper = document.getElementById('app')
if (wrapper) ReactDOM.render(<App />, wrapper)
