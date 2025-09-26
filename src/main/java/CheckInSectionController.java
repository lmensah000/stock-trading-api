//public class CheckInSectionController {
//
//    @POST
//    @Path(URI_TOKEN_KEY + URI_POSTFIX_CHANGE)
//    @Produces(Siren4J.JSON_MEDIATYPE)
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    @PreAuthorize("@customSecurityExpressions.hasPerformanceAppPermission(#" + PATH_PARAM_AOID
//            + ")")
//    @IndirectManagerRestricted
//    public Response updateCheckInSection(@ApiParam(value = "Checkin Section Key") @PathParam(PATH_PARAM_KEY) final long key,
//                                         @ApiParam(value = "AOID of the subject of the evaluation") @PathParam(PATH_PARAM_AOID) final String associateoid,
//                                         final MultivaluedMap<String, String> formParams) {
//        String comment = formParams.containsKey(COMMENT) ? formParams.getFirst(COMMENT) : null;
//
//        /* get existing check-in section based on the key passed */
//        final CheckInSection checkInSection = checkInSectionManagerService.getCheckInSection(key);
//        Response response;
//        Boolean commentRequired = formParams.containsKey(COMMENT_REQUIRED) ? Boolean.parseBoolean(formParams.getFirst(COMMENT_REQUIRED))
//                                                                           : Boolean.TRUE;
//
//        final boolean showFeedbackCommentToEmployee = formParams.containsKey(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE) ? Boolean.parseBoolean(formParams.getFirst(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE)
//                                                                                                                                                 .trim())
//                                                                                                                : Boolean.TRUE;
//
//        /* validate the check-in section comment */
//        if(formParams.containsKey(COMMENT)) {
//            RestParameterValidator.validateText(COMMENT, comment, commentRequired,
//                                                CheckInSectionResource.COMMENT_MAX_LENGTH);
//        }
//
//        final boolean showFeedbackCommentToEmployee = formParams.containsKey(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE) ? Boolean.parseBoolean(formParams.getFirst(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE)
//                .trim())
//                : true;
//
//        /* check if check-in section is existing */
//        if(checkInSection == null) {
//            LOG.warn(new StringBuilder(CHECK_IN_SECTION_NOT_FOUND).append(key).toString());
//
//            response = Response.status(HttpStatus.SC_NOT_FOUND)
//                    .entity(new StringBuilder(CHECK_IN_SECTION_NOT_FOUND).append(key))
//                    .build();
//        } else {
//
//            /* validate the check-in section comment */
//            if(checkInSection.getCheckInSectionDefinition() != null) {
//                Boolean commentRequired = checkInSection.getCheckInSectionDefinition().isCommentRequired();
//
//                RestParameterValidator.validateText(COMMENT, comment, commentRequired,
//                        CheckInSectionResource.COMMENT_MAX_LENGTH);
//            }
//
//            /* check if check-in evaluation is viewable */
//            final CheckInEvaluation checkInEval = checkInSection.getCheckInEvaluation();
//
//            if(checkInEval == null) {
//                return Response.status(Response.Status.NOT_FOUND).build();
//            } else if(!checkInRetrieveAndResponseService.isCheckInEvalViewable(checkInEval,
//                    associateoid)) {
//                LOG.warn(new StringBuilder(CHECK_IN_EVAL_PRE_MESSAGE).append(checkInEval.getKey())
//                        .append(CHECKIN_EVAL_IS_NOT_VIEWABLE_TO_USER)
//                        .append(associateoid));
//                return Response.status(HttpStatus.SC_BAD_REQUEST).build();
//            }
//
//            /* when check-in section exists, update its comment with the one passed */
//            try {
//                /* Call to update check-in section comment and showFeedbackCommentToEmployee flag if it exists in form params */
//                if(formParams.containsKey(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE)){
//                    checkInSectionManagerService.updateCheckInSection(checkInSection, comment, showFeedbackCommentToEmployee);
//                }
//                else{
//                    checkInSectionManagerService.updateCheckInSection(checkInSection, comment);
//                }
//
//                response = createOkResponse(checkInSectionResourceBuilder.buildCheckInSectionResource(checkInSection));
//            } catch (UnsupportedOperationException unsupportedOperationException) {
//                /*
//                 * comes here when the manager updating is not direct manager or the check-in
//                 * associated with the section is already PUBLISHED.
//                 */
//                LOG.warn(new StringBuilder(CHECK_IN_SECTION_FORBIDDEN).append(key),
//                        unsupportedOperationException);
//                response = Response.status(HttpStatus.SC_FORBIDDEN)
//                        .entity(new StringBuilder(CHECK_IN_SECTION_FORBIDDEN).append(key))
//                        .build();
//            }
//
//        }
//
//        return response;
//    }
//}
//
//    @POST
//    @Path(URI_TOKEN_KEY + URI_POSTFIX_CHANGE)
//    @Produces(Siren4J.JSON_MEDIATYPE)
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    @PreAuthorize("@customSecurityExpressions.hasPerformanceAppPermission(#" + PATH_PARAM_AOID
//            + ")")
//    @IndirectManagerRestricted
//    public Response updateCheckInSection(@ApiParam(value = "Checkin Section Key") @PathParam(PATH_PARAM_KEY) final long key,
//                                         @ApiParam(value = "AOID of the subject of the evaluation") @PathParam(PATH_PARAM_AOID) final String associateoid,
//                                         final MultivaluedMap<String, String> formParams) {
//        String comment = formParams.containsKey(COMMENT) ? formParams.getFirst(COMMENT) : null;
//
//        /* get existing check-in section based on the key passed */
//        final CheckInSection checkInSection = checkInSectionManagerService.getCheckInSection(key);
//        Response response;
//
//        final boolean showFeedbackCommentToEmployee = formParams.containsKey(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE) ? Boolean.parseBoolean(formParams.getFirst(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE)
//                .trim())
//                : Boolean.TRUE;
//
//        /* check if check-in section is existing */
//        if(checkInSection == null) {
//            LOG.warn(new StringBuilder(CHECK_IN_SECTION_NOT_FOUND).append(key).toString());
//
//            response = Response.status(HttpStatus.SC_NOT_FOUND)
//                    .entity(new StringBuilder(CHECK_IN_SECTION_NOT_FOUND).append(key))
//                    .build();
//        } else {
//
//            /* validate the check-in section comment */
//            if(checkInSection.getCheckInSectionDefinition() != null) {
//                Boolean commentRequired = checkInSection.getCheckInSectionDefinition().isCommentRequired();
//
//                RestParameterValidator.validateText(COMMENT, comment, commentRequired,
//                        CheckInSectionResource.COMMENT_MAX_LENGTH);
//            }
//
//            /* check if check-in evaluation is viewable */
//            final CheckInEvaluation checkInEval = checkInSection.getCheckInEvaluation();
//
//            if(checkInEval == null) {
//                return Response.status(Response.Status.NOT_FOUND).build();
//            } else if(!checkInRetrieveAndResponseService.isCheckInEvalViewable(checkInEval,
//                    associateoid)) {
//                LOG.warn(new StringBuilder(CHECK_IN_EVAL_PRE_MESSAGE).append(checkInEval.getKey())
//                        .append(CHECKIN_EVAL_IS_NOT_VIEWABLE_TO_USER)
//                        .append(associateoid));
//                return Response.status(HttpStatus.SC_BAD_REQUEST).build();
//            }
//
//            /* when check-in section exists, update its comment with the one passed */
//            try {
//                /* Call to update check-in section comment and showFeedbackCommentToEmployee flag if it exists in form params */
//                if(formParams.containsKey(SHOW_FEEDBACK_COMMENT_TO_EMPLOYEE)){
//                    checkInSectionManagerService.updateCheckInSection(checkInSection, comment, showFeedbackCommentToEmployee);
//                }
//                else{
//                    checkInSectionManagerService.updateCheckInSection(checkInSection, comment);
//                }
//
//                response = createOkResponse(checkInSectionResourceBuilder.buildCheckInSectionResource(checkInSection));
//            } catch (UnsupportedOperationException unsupportedOperationException) {
//                /*
//                 * comes here when the manager updating is not direct manager or the check-in
//                 * associated with the section is already PUBLISHED.
//                 */
//                LOG.warn(new StringBuilder(CHECK_IN_SECTION_FORBIDDEN).append(key),
//                        unsupportedOperationException);
//                response = Response.status(HttpStatus.SC_FORBIDDEN)
//                        .entity(new StringBuilder(CHECK_IN_SECTION_FORBIDDEN).append(key))
//                        .build();
//            }
//
//        }
//
//        return response;
//
//    }
//
