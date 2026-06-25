// frontend/src/components/ProductCard.test.js
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { CartProvider } from '../context/CartContext';
import ProductCard from './ProductCard';

// Helper: renderiza el componente ya envuelto en CartProvider
const renderWithCart = (ui) => render(<CartProvider>{ui}</CartProvider>);

const productoMock = {
  sku: 'SKU-001',
  nombre: 'Laptop Test',
  precio: 999.99,
  stockActual: 5,
  disponible: true,
};

const productoAgotado = {
  sku: 'SKU-002',
  nombre: 'Producto Agotado',
  precio: 100,
  stockActual: 0,
  disponible: false,
};

test('muestra el nombre y SKU del producto', () => {
  renderWithCart(<ProductCard producto={productoMock} />);

  expect(screen.getByText('Laptop Test')).toBeInTheDocument();
  expect(screen.getByText('SKU-001')).toBeInTheDocument();
});

test('muestra el precio del producto', () => {
  renderWithCart(<ProductCard producto={productoMock} />);

  expect(screen.getByText(/999/)).toBeInTheDocument();
});

test('el botón "Agregar" está habilitado cuando hay stock', () => {
  renderWithCart(<ProductCard producto={productoMock} />);

  const boton = screen.getByRole('button');
  expect(boton).not.toBeDisabled();
  expect(boton).toHaveTextContent('Agregar');
});

test('el botón está deshabilitado cuando el producto está agotado', () => {
  renderWithCart(<ProductCard producto={productoAgotado} />);

  const boton = screen.getByRole('button');
  expect(boton).toBeDisabled();
});

test('agrega el producto al carrito al hacer click', () => {
  renderWithCart(<ProductCard producto={productoMock} />);

  const boton = screen.getByRole('button');
  fireEvent.click(boton);

  // Después del click, el botón debe mostrar "En carrito (1)"
  expect(screen.getByRole('button')).toHaveTextContent('En carrito (1)');
});