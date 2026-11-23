import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import {VitePWA} from 'vite-plugin-pwa';

export default defineConfig({
  plugins:[
    react(),
    tailwindcss(),
    VitePWA({
      registerType:'autoUpdate',
      manifest:{
        name:'ICMAP Smart Tutor Portal',
        short_name:'ICMAP Tutor',
        start_url:'/',
        display:'standalone',
        theme_color:'#07111f',
        background_color:'#07111f',
        icons:[{src:'/icon.svg',sizes:'any',type:'image/svg+xml'}]
      }
    })
  ]
});