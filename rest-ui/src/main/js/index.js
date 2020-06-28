import loadable from '@loadable/component'
import React from 'react'
import ReactDOM from 'react-dom'

const App = loadable(() => import('./components/ui'))

require('./css/main.css')
require('./css/swagger-ui.css')

const wrapper = document.getElementById('app')
if (wrapper) ReactDOM.render(<App />, wrapper)
