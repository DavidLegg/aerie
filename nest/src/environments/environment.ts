/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

const windowBaseUrl = `http://localhost`;

export const environment = {
  adaptationServiceBaseUrl: `${windowBaseUrl}:27182/api`,
  // baseUrl: 'https://leucadia.jpl.nasa.gov:9443',
  baseUrl: 'https://localhost:8443',
  planServiceBaseUrl: `${windowBaseUrl}:27183/api`,
  production: false,
  sequencingServiceBaseUrl: `${windowBaseUrl}:27186/sequencing`,
};
