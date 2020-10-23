import React from 'react'
import ReactDOM from 'react-dom'
import App from './components/ui'

import './css/main.css'
import './css/swagger-ui.css'

const wrapper = document.getElementById('app')
if (wrapper) ReactDOM.render(<App />, wrapper)
